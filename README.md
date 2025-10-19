# TNPSC Mock Test Application

A professional Flutter web application for Tamil Nadu Public Service Commission (TNPSC) mock tests with a modern, mobile-responsive UI design.

## Features

### 📱 Current Pages

1. **Login Page**
   - Clean, professional UI with mobile number authentication
   - Form validation
   - Guest login option
   - Smooth animations and transitions

2. **OTP Verification Page**
   - 6-digit OTP input with auto-focus
   - Resend OTP functionality with countdown timer
   - Auto-verification on completion
   - Helpful user guidance

3. **Home Page**
   - User dashboard with statistics
   - Quick action buttons
   - Test categories with progress tracking
   - Recent test history
   - Bottom navigation bar

### 🎨 Design Features

- **Professional Light Mode Theme**
- **Mobile-First Design** (looks like a mobile app)
- **Responsive Layout** (max-width: 450px for mobile app feel)
- **Modern UI Components**:
  - Gradient buttons and cards
  - Smooth shadows and elevations
  - Custom color scheme with green primary color
  - Google Fonts (Poppins) integration
  - Material Design 3 principles

## Getting Started

### Prerequisites

- Flutter SDK (3.0.0 or higher)
- Web browser (Chrome recommended for development)

### Installation

1. Navigate to the project directory:
   ```bash
   cd MockTest
   ```

2. Get the dependencies:
   ```bash
   flutter pub get
   ```

3. Run the application:
   ```bash
   flutter run -d chrome --web-port 8080
   ```

   Or for web-server mode:
   ```bash
   flutter run -d web-server --web-port 8080
   ```

4. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```

### Building for Production

To build the web app for production:

```bash
flutter build web --release
```

The built files will be in the `build/web` directory.

## Project Structure

```
lib/
├── main.dart                 # App entry point
├── utils/
│   └── app_colors.dart      # Color constants
└── screens/
    ├── login_page.dart      # Login screen
    ├── otp_verification_page.dart  # OTP verification
    └── home_page.dart       # Home dashboard
```

## Color Scheme

- **Primary Color**: `#2E7D32` (Green)
- **Secondary Color**: `#1976D2` (Blue)
- **Background**: `#F5F7FA` (Light Gray)
- **Success**: `#4CAF50`
- **Warning**: `#FF9800`
- **Error**: `#F44336`

## Dependencies

- `flutter`: SDK
- `google_fonts`: ^6.1.0 - Custom fonts
- `flutter_svg`: ^2.0.9 - SVG support (for future icons)
- `cupertino_icons`: ^1.0.2 - iOS style icons

## Future Enhancements

- [ ] Implement actual test-taking functionality
- [ ] Add analytics and progress tracking
- [ ] Integrate backend API
- [ ] Add more test categories
- [ ] Implement user profile management
- [ ] Add exam preparation materials
- [ ] Dark mode support
- [ ] Performance analytics charts
- [ ] Social sharing features
- [ ] Leaderboard

## Development Tips

### For Mobile App Experience

The app is designed to look like a mobile application:
- Maximum width constraint of 450px centers the content
- Portrait-optimized layouts
- Touch-friendly button sizes
- Mobile-first component design

### Customization

To customize the color scheme, edit `lib/utils/app_colors.dart`.

To modify the theme, edit the `ThemeData` in `lib/main.dart`.

## License

This project is created for educational purposes.

## Contact

For questions or support regarding TNPSC preparation, please contact the relevant authorities.

---

**Happy Learning! 📚**


