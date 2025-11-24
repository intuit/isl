#!/usr/bin/env node

/**
 * Helper script to generate pre-encoded playground URLs
 * Usage: node encode-playground-url.js
 */

const readline = require('readline');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function encodeBase64Url(str) {
  // Convert to base64
  const base64 = Buffer.from(str).toString('base64');
  // Make it URL-safe
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

console.log('🎮 ISL Playground URL Generator\n');

rl.question('Enter ISL code:\n', (isl) => {
  rl.question('\nEnter Input JSON (or press Enter for {}):\n', (input) => {
    const inputJson = input.trim() || '{}';
    const playgroundUrl = 'https://isl-playground.up.railway.app';
    
    const islEncoded = encodeBase64Url(isl);
    const inputEncoded = encodeBase64Url(inputJson);
    
    const fullUrl = `${playgroundUrl}?isl_encoded=${islEncoded}&input_encoded=${inputEncoded}`;
    
    console.log('\n✅ Generated URL:\n');
    console.log(fullUrl);
    console.log('\n📋 HTML Button:\n');
    console.log(`<div class="playground-button-container">
  <a href="${fullUrl}" 
     target="_blank"
     rel="noopener noreferrer"
     class="btn-playground">
    ▶️ Run in Playground
  </a>
</div>`);
    
    rl.close();
  });
});

