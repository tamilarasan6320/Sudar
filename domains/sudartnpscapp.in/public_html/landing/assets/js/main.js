/**
 * SUDAR Landing Page - Main JavaScript
 * =====================================
 */

(function() {
    'use strict';

    // ========================================
    // DOM Elements
    // ========================================
    const navbar = document.getElementById('navbar');
    const navToggle = document.getElementById('navToggle');
    const navMenu = document.getElementById('navMenu');
    const navLinks = document.querySelectorAll('.nav-link');
    const faqItems = document.querySelectorAll('.faq-item');
    
    // Play Store buttons
    const playStoreButtons = [
        document.getElementById('navPlayStore'),
        document.getElementById('heroPlayStore'),
        document.getElementById('stepsPlayStore'),
        document.getElementById('finalPlayStore')
    ];

    // ========================================
    // Initialize Play Store Links
    // ========================================
    function initPlayStoreLinks() {
        const url = window.SUDAR_CONFIG?.PLAY_STORE_URL || '#';
        
        playStoreButtons.forEach(btn => {
            if (btn) {
                btn.href = url;
                
                // If it's a placeholder, show a tooltip or message on click
                if (url === '#') {
                    btn.addEventListener('click', function(e) {
                        e.preventDefault();
                        alert('Coming soon to Google Play Store! Stay tuned.');
                    });
                }
            }
        });
    }

    // ========================================
    // Navbar Scroll Effect
    // ========================================
    function handleNavbarScroll() {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    }

    // ========================================
    // Mobile Navigation Toggle
    // ========================================
    function toggleMobileNav() {
        navToggle.classList.toggle('active');
        navMenu.classList.toggle('active');
        document.body.style.overflow = navMenu.classList.contains('active') ? 'hidden' : '';
    }

    function closeMobileNav() {
        navToggle.classList.remove('active');
        navMenu.classList.remove('active');
        document.body.style.overflow = '';
    }

    // ========================================
    // Smooth Scroll for Nav Links
    // ========================================
    function handleNavLinkClick(e) {
        const href = this.getAttribute('href');
        
        if (href.startsWith('#')) {
            e.preventDefault();
            const target = document.querySelector(href);
            
            if (target) {
                const navHeight = navbar.offsetHeight;
                const targetPosition = target.getBoundingClientRect().top + window.pageYOffset - navHeight;
                
                window.scrollTo({
                    top: targetPosition,
                    behavior: 'smooth'
                });
                
                // Close mobile nav if open
                closeMobileNav();
            }
        }
    }

    // ========================================
    // FAQ Accordion
    // ========================================
    function handleFaqClick() {
        const isActive = this.classList.contains('active');
        
        // Close all FAQ items
        faqItems.forEach(item => {
            item.classList.remove('active');
        });
        
        // Open clicked item if it wasn't active
        if (!isActive) {
            this.classList.add('active');
        }
    }

    // ========================================
    // Intersection Observer for Animations
    // ========================================
    function initScrollAnimations() {
        const animatedElements = document.querySelectorAll('.feature-card, .category-card, .step-card, .faq-item');
        
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.style.animationPlayState = 'running';
                    observer.unobserve(entry.target);
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        });
        
        animatedElements.forEach(el => {
            el.style.animationPlayState = 'paused';
            observer.observe(el);
        });
    }

    // ========================================
    // Event Listeners
    // ========================================
    function initEventListeners() {
        // Navbar scroll
        window.addEventListener('scroll', handleNavbarScroll, { passive: true });
        
        // Mobile nav toggle
        if (navToggle) {
            navToggle.addEventListener('click', toggleMobileNav);
        }
        
        // Nav links smooth scroll
        navLinks.forEach(link => {
            link.addEventListener('click', handleNavLinkClick);
        });
        
        // FAQ accordion
        faqItems.forEach(item => {
            const question = item.querySelector('.faq-question');
            if (question) {
                question.addEventListener('click', handleFaqClick.bind(item));
            }
        });
        
        // Close mobile nav on resize to desktop
        window.addEventListener('resize', () => {
            if (window.innerWidth > 768) {
                closeMobileNav();
            }
        });
        
        // Close mobile nav when clicking outside
        document.addEventListener('click', (e) => {
            if (navMenu.classList.contains('active') && 
                !navMenu.contains(e.target) && 
                !navToggle.contains(e.target)) {
                closeMobileNav();
            }
        });
    }

    // ========================================
    // Initialize
    // ========================================
    function init() {
        initPlayStoreLinks();
        initEventListeners();
        initScrollAnimations();
        handleNavbarScroll(); // Check initial scroll position
    }

    // Run on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();


