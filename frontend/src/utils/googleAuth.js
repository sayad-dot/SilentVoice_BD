// utils/googleAuth.js - FIXED VERSION
export const initializeGoogleAuth = () => {
  return new Promise((resolve, reject) => {
    // Check if already loaded
    if (window.google && window.google.accounts) {
      resolve(window.google);
      return;
    }

    // Remove any existing Google script
    const existingScript = document.getElementById('google-gsi-script');
    if (existingScript) {
      existingScript.remove();
    }

    // Create and load the script
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.id = 'google-gsi-script';
    
    script.onload = () => {
      // Simple check with retries
      let attempts = 0;
      const checkGoogle = () => {
        if (window.google && window.google.accounts) {
          console.log('✅ Google loaded successfully');
          resolve(window.google);
        } else if (attempts < 30) {
          attempts++;
          setTimeout(checkGoogle, 200);
        } else {
          reject(new Error('Google failed to load'));
        }
      };
      checkGoogle();
    };
    
    script.onerror = () => reject(new Error('Script loading failed'));
    document.head.appendChild(script);
  });
};
