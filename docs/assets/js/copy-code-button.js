/**
 * Copy Button Injector for Code Blocks
 * Adds a "Copy" button to all code blocks for easy copying
 */

(function() {
  'use strict';
  
  /**
   * Creates a copy button
   */
  function createCopyButton(codeElement) {
    const button = document.createElement('button');
    button.className = 'btn-copy';
    button.innerHTML = '📋 Copy';
    button.title = 'Copy code to clipboard';
    button.setAttribute('aria-label', 'Copy code to clipboard');
    
    button.addEventListener('click', async function(e) {
      e.preventDefault();
      
      const code = codeElement.textContent || '';
      
      try {
        // Try modern clipboard API first
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(code);
        } else {
          // Fallback for older browsers or non-HTTPS
          const textArea = document.createElement('textarea');
          textArea.value = code;
          textArea.style.position = 'fixed';
          textArea.style.left = '-999999px';
          textArea.style.top = '-999999px';
          document.body.appendChild(textArea);
          textArea.focus();
          textArea.select();
          document.execCommand('copy');
          textArea.remove();
        }
        
        // Show success feedback
        button.innerHTML = '✓ Copied!';
        button.classList.add('copied');
        
        setTimeout(function() {
          button.innerHTML = '📋 Copy';
          button.classList.remove('copied');
        }, 2000);
        
      } catch (err) {
        console.error('Failed to copy:', err);
        button.innerHTML = '✗ Failed';
        button.classList.add('error');
        
        setTimeout(function() {
          button.innerHTML = '📋 Copy';
          button.classList.remove('error');
        }, 2000);
      }
    });
    
    return button;
  }
  
  /**
   * Initialize - add copy buttons to all code blocks
   */
  function initialize() {
    const allPreElements = document.querySelectorAll('pre');
    let buttonsAdded = 0;
    
    allPreElements.forEach(function(preElement) {
      // Skip if copy button already exists
      if (preElement.querySelector('.btn-copy')) {
        return;
      }
      
      const codeElement = preElement.querySelector('code');
      if (!codeElement) return;
      
      // Skip if code block is empty
      const code = codeElement.textContent.trim();
      if (!code) return;
      
      // Make pre element positioned so button can be absolute
      preElement.style.position = 'relative';
      
      // Create and insert copy button
      const button = createCopyButton(codeElement);
      preElement.appendChild(button);
      buttonsAdded++;
    });
    
    if (buttonsAdded > 0) {
      console.log(`Copy Buttons: Added ${buttonsAdded} button(s)`);
    }
  }
  
  // Run when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize);
  } else {
    initialize();
  }
  
  // Export for manual use if needed
  window.CodeCopyButtons = {
    initialize: initialize
  };
})();


