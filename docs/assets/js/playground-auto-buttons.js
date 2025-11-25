/**
 * ISL Playground Auto-Button Injector
 * Runs on page load to add "Run in Playground" buttons after ISL code blocks
 */

(function() {
  'use strict';
  
  const PLAYGROUND_URL = 'https://isl-playground.up.railway.app';
  
  /**
   * Encodes a string to URL-safe base64
   */
  function encodeBase64Url(str) {
    try {
      const base64 = btoa(unescape(encodeURIComponent(str)));
      return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
    } catch (err) {
      console.error('Failed to encode:', err);
      return '';
    }
  }
  
  /**
   * Checks if a code block is explicitly marked as ISL
   */
  function hasIslClass(codeElement, preElement) {
    // Check code element classes
    const codeClasses = codeElement.className || '';
    if (codeClasses.includes('language-isl') || codeClasses.includes('highlighter-isl')) {
      return true;
    }
    
    // Check pre element classes
    const preClasses = preElement.className || '';
    if (preClasses.includes('language-isl') || preClasses.includes('highlighter-isl')) {
      return true;
    }
    
    return false;
  }
  
  /**
   * Checks if a code block contains ISL code patterns
   */
  function hasIslPatterns(text) {
    // More specific ISL patterns to avoid false positives
    const patterns = [
      /\$input\./, // $input.property access
      /fun\s+\w+\s*\([^)]*\$/, // function with $ parameter
      /\|\s*(map|filter|split|trim|upper|lower|length|sum|first|last|flatten|distinct|sort|join|reverse)/, // ISL modifiers
      /foreach\s+\$\w+\s+in/, // foreach loops
      /@\.\w+\(/, // @ function calls
      /result\s*:/, // common ISL result pattern
    ];
    
    return patterns.some(pattern => pattern.test(text));
  }
  
  /**
   * Checks if a code block contains ISL code
   * Now checks both explicit language markup AND content patterns
   */
  function isIslCode(codeElement, preElement) {
    // First check: Is it explicitly marked as ISL?
    if (hasIslClass(codeElement, preElement)) {
      return true;
    }
    
    // Second check: Does it have ISL patterns in the content?
    const text = codeElement.textContent || '';
    if (text.length > 10 && hasIslPatterns(text)) {
      // Additional validation: if it looks like other languages, exclude it
      // Exclude if it looks like shell/bash
      if (text.trim().startsWith('#!') || /^(npm|cd|ls|mkdir|curl|git|echo|export)\s/.test(text)) {
        return false;
      }
      // Exclude if it looks like pure JSON
      if ((text.trim().startsWith('{') || text.trim().startsWith('[')) && 
          !text.includes('$') && !text.includes('fun ')) {
        return false;
      }
      // Exclude if it looks like Java/Kotlin
      if (/\b(public|private|class|import|package|void)\b/.test(text)) {
        return false;
      }
      
      return true;
    }
    
    return false;
  }
  
  /**
   * Finds the "Input JSON" code block before an ISL block
   */
  function findInputJson(islPre) {
    let currentElement = islPre.previousElementSibling;
    let iterations = 0;
    
    // Look back up to 10 elements
    while (currentElement && iterations < 10) {
      iterations++;
      
      // Check if this is a <pre> with JSON code
      if (currentElement.tagName === 'PRE') {
        const codeElement = currentElement.querySelector('code');
        if (codeElement) {
          const text = codeElement.textContent.trim();
          // Simple check if it looks like JSON
          if (text.startsWith('{') || text.startsWith('[')) {
            // Check if there's an "Input JSON" label before this
            let labelElement = currentElement.previousElementSibling;
            let labelChecks = 0;
            
            while (labelElement && labelChecks < 3) {
              labelChecks++;
              const labelText = labelElement.textContent || '';
              
              if (/\w*input\w*/i.test(labelText)) {
                return text;
              }
              
              labelElement = labelElement.previousElementSibling;
            }
          }
        }
      }
      
      currentElement = currentElement.previousElementSibling;
    }
    
    return '{}';
  }
  
  /**
   * Creates a playground button
   */
  function createButton(islCode, inputJson) {
    const islEncoded = encodeBase64Url(islCode);
    const inputEncoded = encodeBase64Url(inputJson);
    const url = `${PLAYGROUND_URL}?isl_encoded=${islEncoded}&input_encoded=${inputEncoded}`;
    
    const button = document.createElement('a');
    button.href = url;
    button.target = '_blank';
    button.rel = 'noopener noreferrer';
    button.className = 'btn-playground';
    button.textContent = '▶️ Run in Playground';
    button.title = 'Open this example in the ISL Playground';
    
    return button;
  }
  
  /**
   * Initialize - add buttons to all ISL code blocks
   */
  function initialize() {
    const allPreElements = document.querySelectorAll('pre');
    let buttonsAdded = 0;
    
    allPreElements.forEach(function(preElement) {
      // Skip if button already exists
      if (preElement.querySelector('.btn-playground')) {
        return;
      }
      
      const codeElement = preElement.querySelector('code');
      if (!codeElement) return;
      
      // Check if this is ISL code (pass both code and pre elements)
      if (!isIslCode(codeElement, preElement)) return;
      
      const islCode = codeElement.textContent.trim();
      if (!islCode) return;
      
      // Find input JSON
      const inputJson = findInputJson(preElement);
      
      // Make pre element positioned so button can be absolute
      preElement.style.position = 'relative';
      
      // Create and insert button as overlay
      const button = createButton(islCode, inputJson);
      preElement.appendChild(button);
      buttonsAdded++;
    });
    
    if (buttonsAdded > 0) {
      console.log(`ISL Playground: Added ${buttonsAdded} button(s)`);
    }
  }
  
  // Run when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initialize);
  } else {
    initialize();
  }
  
  // Export for manual use if needed
  window.ISLPlayground = {
    initialize: initialize,
    encodeBase64Url: encodeBase64Url
  };
})();


