/**
 * ISL Playground Helper
 * Provides utilities for creating "Run in Playground" buttons in the documentation
 */

(function() {
  // Base URL for the playground - configurable for different environments
  const PLAYGROUND_BASE_URL = 'https://isl-playground.up.railway.app';
  
  /**
   * Encodes a string to URL-safe base64
   * @param {string} str - The string to encode
   * @returns {string} The base64-encoded string
   */
  function encodeBase64Url(str) {
    try {
      // Convert to base64 and make it URL-safe
      const base64 = btoa(unescape(encodeURIComponent(str)));
      return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
    } catch (err) {
      console.error('Failed to encode to base64:', err);
      return '';
    }
  }
  
  /**
   * Generates a playground URL with ISL code and input JSON
   * @param {string} islCode - The ISL code to load
   * @param {string} inputJson - The input JSON to load (optional)
   * @returns {string} The complete playground URL
   */
  function generatePlaygroundUrl(islCode, inputJson) {
    inputJson = inputJson || '{}';
    
    const params = new URLSearchParams();
    params.set('isl_encoded', encodeBase64Url(islCode));
    params.set('input_encoded', encodeBase64Url(inputJson));
    
    return `${PLAYGROUND_BASE_URL}?${params.toString()}`;
  }
  
  /**
   * Creates a "Run in Playground" button
   * @param {string} islCode - The ISL code to load
   * @param {string} inputJson - The input JSON to load (optional)
   * @returns {HTMLElement} The button element
   */
  function createPlaygroundButton(islCode, inputJson) {
    const url = generatePlaygroundUrl(islCode, inputJson);
    
    const button = document.createElement('a');
    button.href = url;
    button.target = '_blank';
    button.rel = 'noopener noreferrer';
    button.className = 'btn-playground';
    button.innerHTML = '▶️ Run in Playground';
    button.title = 'Open this example in the ISL Playground';
    
    return button;
  }
  
  /**
   * Automatically adds "Run in Playground" buttons to code examples
   * Looks for elements with data-isl and data-input attributes
   */
  function initializePlaygroundButtons() {
    // Find all ISL code blocks with the playground attribute
    const codeBlocks = document.querySelectorAll('[data-playground-isl]');
    
    codeBlocks.forEach(block => {
      const islCode = block.getAttribute('data-playground-isl') || block.textContent || '';
      const inputJson = block.getAttribute('data-playground-input') || '{}';
      
      // Create and insert the button
      const button = createPlaygroundButton(islCode, inputJson);
      
      // Insert after the code block's parent (usually a pre or div)
      const parent = block.closest('pre, div.highlight');
      if (parent && parent.parentNode) {
        const container = document.createElement('div');
        container.className = 'playground-button-container';
        container.appendChild(button);
        parent.parentNode.insertBefore(container, parent.nextSibling);
      }
    });
  }
  
  // Export functions to global scope
  window.ISLPlayground = {
    generateUrl: generatePlaygroundUrl,
    createButton: createPlaygroundButton,
    initialize: initializePlaygroundButtons,
    encodeBase64Url: encodeBase64Url
  };
  
  // Auto-initialize when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializePlaygroundButtons);
  } else {
    initializePlaygroundButtons();
  }
})();

