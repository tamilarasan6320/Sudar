// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:tnpsc_mock_test/main.dart';
import 'package:tnpsc_mock_test/services/theme_service.dart';

void main() {
  testWidgets('App loads successfully', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => ThemeService(),
        child: const TNPSCMockTestApp(),
      ),
    );

    // Verify that the app loads (splash screen should be visible)
    await tester.pumpAndSettle();
    
    // Basic smoke test - app should build without errors
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
