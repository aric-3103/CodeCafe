Build Your Own Background Removal Tool


Create a user-friendly application that removes backgrounds from images locally using AI and allows users to download the processed results individually or as a ZIP archive.

Requirements:

    1.Core Functionality
Allow users to upload a single image and remove its background.
Allow users to upload multiple images for batch processing.
Automatically detect and process supported image formats (.jpg, .jpeg, .png, .webp).
Generate transparent PNG outputs for all processed images.
Perform all background removal locally without using external background-removal APIs.
Display a success message when processing is completed.


    2. User Interface
Provide a clean and intuitive graphical user interface.
Display image previews before processing.
Display processed images after background removal.
Clearly indicate processing status (idle, processing, completed, failed).
Show progress while batch processing multiple images.


    3. Downloads
Allow users to download each processed image individually.
Provide a single-click option to download all processed images as a ZIP file.
Ensure downloaded files retain meaningful filenames.


    4. Error Handling
Gracefully handle unsupported file formats.
Display user-friendly error messages for corrupted or unreadable images.
Continue processing remaining images even if one file fails.
Show a processing summary including: Total files uploaded, successfully processed files, Failed files, Skipped files
