# Question Scraper Guide

## Overview
The Question Scraper is a powerful tool that allows you to automatically import questions from civilserviceaspirants.in website into your TNPSC Mock Test admin panel.

## Features
- ✅ **Multi-Page Scraping**: Automatically detects and scrapes all pages (up to 10 pages)
- ✅ **Beautiful UI**: Modern, intuitive interface with progress tracking
- ✅ **Question Preview**: See all scraped questions before saving
- ✅ **Export Options**: Export as JSON or save directly to database
- ✅ **Tamil Language Support**: Full support for Tamil content
- ✅ **Error Handling**: Robust error handling and validation

## How to Use

### Step 1: Access the Scraper
1. Open your admin panel: `http://localhost/Mock_test/admin/`
2. Click on **"Question Scraper"** in the sidebar menu

### Step 2: Get the URL
1. Visit [civilserviceaspirants.in](https://civilserviceaspirants.in)
2. Navigate to TNPSC Group 4 VAO section
3. Choose a topic (e.g., சேர்த்து எழுதுதல், பிரித்து எழுதுதல்)
4. Copy the URL from the browser

### Step 3: Scrape Questions
1. Paste the URL in the **"Question Page URL"** field
2. Click **"Scrape Questions"** button
3. Wait for the progress bar to complete
4. Review the scraped questions

### Step 4: Save or Export
**Option 1: Export as JSON**
- Click **"Export JSON"** button
- JSON file will be downloaded automatically
- Use this for backup or custom processing

**Option 2: Save to Database**
- Click **"Save to Database"** button
- Questions will be added to your question bank
- Success message will confirm the operation

## Supported URLs

The scraper works with URLs from **civilserviceaspirants.in** in this format:

```
https://civilserviceaspirants.in/TNPSC-Group-4-VAO-Syllabus/[category]/[topic]-1.php
```

### Example URLs:
1. **சேர்த்து எழுதுதல் (Combining Words)**
   ```
   https://civilserviceaspirants.in/TNPSC-Group-4-VAO-Syllabus/பொதுத்-தமிழ்-அலகு-i-இலக்கணம்/சேர்த்து-எழுதுதல்-1.php
   ```

2. **பிரித்து எழுதுதல் (Splitting Words)**
   ```
   https://civilserviceaspirants.in/TNPSC-Group-4-VAO-Syllabus/பொதுத்-தமிழ்-அலகு-i-இலக்கணம்/பிரித்து-எழுதுதல்-1.php
   ```

## How It Works

### Backend Process
1. **URL Validation**: Checks if the URL is from civilserviceaspirants.in
2. **Page Detection**: Automatically detects pagination (e.g., -1.php, -2.php, etc.)
3. **Content Fetching**: Uses cURL to fetch HTML content
4. **Parsing**: Extracts questions, options, and correct answers
5. **Multi-Page**: Continues scraping until no more pages are found
6. **Response**: Returns structured JSON data

### Data Structure
Each scraped question contains:
```json
{
  "question": "சேர்த்து எழுதுக : மற்று + ஓர்",
  "options": {
    "A": "மற்றுஓர்",
    "B": "மறுஓர்",
    "C": "மற்றோர்",
    "D": "மற்றூர்"
  },
  "correctAnswer": "C"
}
```

## Troubleshooting

### Issue: "Failed to fetch content"
**Solutions:**
- Check your internet connection
- Verify XAMPP Apache is running
- Ensure PHP cURL extension is enabled
- Check if the URL is accessible in browser

### Issue: "No questions found"
**Solutions:**
- Verify the URL is correct
- Check if the website structure has changed
- Try a different topic URL
- Ensure you're using the first page (-1.php)

### Issue: "Scraper not working"
**Solutions:**
1. Check PHP error logs: `xampp/apache/logs/error.log`
2. Verify PHP extensions:
   - Open `php.ini`
   - Uncomment: `extension=curl`
   - Uncomment: `extension=mbstring`
   - Restart Apache

### Issue: "CORS Error"
**Solution:**
- The scraper.php already includes CORS headers
- If issues persist, check browser console
- Ensure you're accessing via `localhost` not file://

## Requirements

### Server Requirements
- ✅ PHP 7.0 or higher
- ✅ cURL extension enabled
- ✅ mbstring extension enabled
- ✅ DOMDocument class available
- ✅ allow_url_fopen enabled (fallback)

### Browser Requirements
- ✅ Modern browser (Chrome, Firefox, Safari, Edge)
- ✅ JavaScript enabled
- ✅ localStorage available

## File Structure

```
admin/
├── index.html              # Main admin interface
├── js/
│   └── script.js           # Scraper frontend logic
├── php/
│   └── scraper.php         # Backend scraping engine
├── css/
│   └── style.css           # Styling
└── SCRAPER_GUIDE.md        # This guide
```

## API Endpoint

### POST `/admin/php/scraper.php`

**Request:**
```json
{
  "url": "https://civilserviceaspirants.in/..."
}
```

**Success Response:**
```json
{
  "success": true,
  "topic": "சேர்த்து எழுதுதல்",
  "category": "பொதுத் தமிழ் - அலகு I : இலக்கணம்",
  "totalPages": 10,
  "questions": [...],
  "message": "Successfully scraped 14 questions from 2 pages"
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Error description"
}
```

## Tips & Best Practices

1. **Start with Page 1**: Always use the first page URL (-1.php)
2. **Check Preview**: Review questions before saving
3. **Export Backup**: Always export JSON before saving
4. **Batch Processing**: Scrape one topic at a time
5. **Server Friendly**: Built-in delay between page requests
6. **Test First**: Try with one topic before bulk scraping

## Future Enhancements

- [ ] Support for more exam categories
- [ ] Automatic duplicate detection
- [ ] Question editing before saving
- [ ] Bilingual question mapping
- [ ] Scheduled auto-scraping
- [ ] Question statistics

## Support

For issues or questions:
1. Check this guide first
2. Review troubleshooting section
3. Check browser console for errors
4. Verify PHP error logs
5. Test with example URLs

## Legal Note

⚠️ **Important**: This scraper is for educational purposes only. Please:
- Respect the source website's terms of service
- Don't overload their servers
- Use reasonable delays between requests
- Give proper attribution to the source
- Comply with copyright laws

## Version History

- **v1.0.0** (October 2025)
  - Initial release
  - Multi-page scraping support
  - JSON export functionality
  - Database save feature
  - Tamil language support

---

**Happy Scraping! 🎓**


