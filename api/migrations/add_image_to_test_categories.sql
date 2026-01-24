-- ============================================
-- Test Category Image Feature - Database Migration
-- ============================================
-- Execute this SQL to add image support to test_categories table
-- Version: 1.0
-- Date: January 2026
-- ============================================

-- Step 1: Add image columns to test_categories table
ALTER TABLE test_categories 
ADD COLUMN image VARCHAR(255) NULL COMMENT 'Image filename' AFTER color,
ADD COLUMN image_path VARCHAR(255) NULL COMMENT 'Full path to image file' AFTER image;

-- Step 2: Add index for better query performance
CREATE INDEX idx_test_categories_image ON test_categories(image);

-- Step 3: Verify the changes
DESCRIBE test_categories;

-- Expected output should include:
-- image       | varchar(255) | YES  |     | NULL
-- image_path  | varchar(255) | YES  |     | NULL

-- ============================================
-- Migration Complete!
-- ============================================
-- Next steps:
-- 1. Verify the uploads/test_categories/ directory exists
-- 2. Clear browser cache (Ctrl+Shift+R)
-- 3. Test the feature in the admin panel
-- ============================================
