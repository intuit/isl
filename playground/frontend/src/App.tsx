import { useState, useEffect, useCallback, useRef } from 'react';
import Editor from '@monaco-editor/react';
import axios from 'axios';
import './App.css';
import { registerIslLanguage } from './isl-language';
import type { editor } from 'monaco-editor';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://isl-playground.up.railway.app/api';

interface TransformResponse {
  success: boolean;
  output?: string;
  error?: {
    message: string;
    line?: number;
    column?: number;
    type: string;
  };
}

interface ValidationResponse {
  valid: boolean;
  errors: Array<{
    message: string;
    line?: number;
    column?: number;
    type: string;
  }>;
}

// Helper to decode base64 URL-safe strings
const decodeBase64Url = (str: string): string => {
  try {
    // Convert URL-safe base64 to standard base64
    const base64 = str.replace(/-/g, '+').replace(/_/g, '/');
    // Add padding if needed
    const padded = base64 + '==='.slice((base64.length + 3) % 4);
    return decodeURIComponent(escape(atob(padded)));
  } catch (err) {
    console.error('Failed to decode base64:', err);
    return '';
  }
};

// Helper to check if ISL code needs wrapping in fun run($input) { }
const needsFunWrapper = (code: string): boolean => {
  // Remove comments and whitespace for checking
  const cleanCode = code.replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '').trim();
  
  // Check if code already has a fun declaration (fun run or any fun)
  const hasFunDeclaration = /^\s*fun\s+\w+\s*\(/.test(cleanCode);
  
  return !hasFunDeclaration;
};

// Helper to wrap ISL code in fun run($input) { } if needed
const ensureFunWrapper = (code: string): string => {
  if (!needsFunWrapper(code)) {
    return code;
  }
  
  // Wrap the code in fun run($input) { }
  return `fun run($input) {\n // Adjust code as required then return a result\n${code.split('\n').map(line => '    ' + line).join('\n')}\n}`;
};

// Helper to load code from URL parameters
const loadFromUrl = () => {
  const params = new URLSearchParams(window.location.search);
  
  let islCode = 'fun run( $input )\n{\n    result: $input.message\n}';
  let inputJson = '{\n  "message": "Hello, ISL!"\n}';
  
  // Support both encoded and plain text formats
  const islParam = params.get('isl');
  const inputParam = params.get('input');
  const islEncodedParam = params.get('isl_encoded');
  const inputEncodedParam = params.get('input_encoded');
  
  if (islEncodedParam) {
    const decoded = decodeBase64Url(islEncodedParam);
    if (decoded) islCode = ensureFunWrapper(decoded);
  } else if (islParam) {
    islCode = ensureFunWrapper(islParam);
  }
  
  if (inputEncodedParam) {
    const decoded = decodeBase64Url(inputEncodedParam);
    if (decoded) inputJson = decoded;
  } else if (inputParam) {
    inputJson = inputParam;
  }
  
  return { islCode, inputJson };
};

// Helper to format error messages - split at "at Position" for better readability
const formatErrorMessage = (message: string): string => {
  // Split at "at Position" and add line break
  return message.replace(/(\s+at Position)/gi, '\n$1');
};

function App() {
  const urlData = loadFromUrl();
  const [islCode, setIslCode] = useState(urlData.islCode);
  const [inputJson, setInputJson] = useState(urlData.inputJson);
  const [output, setOutput] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [validationSuccess, setValidationSuccess] = useState(false);
  
  // Ref to store Monaco editor instance
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);
  const decorationsRef = useRef<string[]>([]);

  // Function to clear error decorations
  const clearErrorDecorations = () => {
    if (editorRef.current && decorationsRef.current.length > 0) {
      decorationsRef.current = editorRef.current.deltaDecorations(decorationsRef.current, []);
    }
  };

  // Function to highlight error in editor
  const highlightError = (line?: number, column?: number) => {
    if (!editorRef.current || !line) return;
    
    // Clear previous decorations
    clearErrorDecorations();
    
    // Create new decoration for error line
    const decorations: editor.IModelDeltaDecoration[] = [
      {
        range: {
          startLineNumber: line,
          startColumn: column || 1,
          endLineNumber: line,
          endColumn: column ? column + 1 : 1000, // Highlight to end of line if no column
        },
        options: {
          isWholeLine: !column, // Highlight whole line if no specific column
          className: 'error-line',
          glyphMarginClassName: 'error-glyph',
          overviewRuler: {
            color: '#ff0000',
            position: 4, // Right side
          },
          minimap: {
            color: '#ff0000',
            position: 2, // Inline
          },
        },
      },
    ];
    
    // Apply decorations
    decorationsRef.current = editorRef.current.deltaDecorations([], decorations);
    
    // Scroll to error line
    editorRef.current.revealLineInCenter(line);
  };

  const handleRun = useCallback(async () => {
    if (loading) return;
    
    setLoading(true);
    setError('');
    setOutput('');
    setValidationErrors([]);
    setValidationSuccess(false);
    clearErrorDecorations(); // Clear previous error highlights

    // Ensure the code has fun run wrapper, and update editor if it was added
    let codeToRun = islCode;
    if (needsFunWrapper(islCode)) {
      codeToRun = ensureFunWrapper(islCode);
      setIslCode(codeToRun); // Update the editor with the wrapped code
    }

    try {
      const response = await axios.post<TransformResponse>(`${API_BASE_URL}/transform`, {
        isl: codeToRun,
        input: inputJson,
      });

      if (response.data.success) {
        setOutput(response.data.output || '');
      } else if (response.data.error) {
        const err = response.data.error;
        const errorMsg = err.line && err.column
          ? `${err.type} at line ${err.line}, column ${err.column}: ${err.message}`
          : `${err.type}: ${err.message}`;
        setError(formatErrorMessage(errorMsg));
        
        // Highlight error in editor
        if (err.line) {
          highlightError(err.line, err.column);
        }
      }
    } catch (err) {
      setError(`Request failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  }, [islCode, inputJson, loading]);

  const handleValidate = useCallback(async () => {
    if (loading) return;
    
    setLoading(true);
    setValidationErrors([]);
    setValidationSuccess(false);
    setError('');
    clearErrorDecorations(); // Clear previous error highlights

    // Ensure the code has fun run wrapper, and update editor if it was added
    let codeToValidate = islCode;
    if (needsFunWrapper(islCode)) {
      codeToValidate = ensureFunWrapper(islCode);
      setIslCode(codeToValidate); // Update the editor with the wrapped code
    }

    try {
      const response = await axios.post<ValidationResponse>(`${API_BASE_URL}/validate`, {
        isl: codeToValidate,
      });

      if (response.data.valid) {
        setError('');
        setValidationErrors([]);
        setValidationSuccess(true);
      } else {
        const errors = response.data.errors.map((err) => {
          return err.line && err.column
            ? `Line ${err.line}, Col ${err.column}: ${err.message}`
            : err.message;
        });
        setValidationErrors(errors);
        
        // Highlight first error in editor
        if (response.data.errors.length > 0) {
          const firstError = response.data.errors[0];
          if (firstError.line) {
            highlightError(firstError.line, firstError.column);
          }
        }
      }
    } catch (err) {
      setError(`Validation request failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  }, [islCode, loading]);

  useEffect(() => {
    // Add keyboard shortcuts
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.key === 'q') {
        e.preventDefault();
        handleValidate();
      } else if (e.ctrlKey && e.key === 'r') {
        e.preventDefault();
        handleRun();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleRun, handleValidate]);

  return (
    <div className="app">
      <header className="header">
        <div className="header-content">
          <img src="/isl-logo.png" alt="ISL Logo" className="header-logo" />
          <div className="header-text">
            <h1>ISL Playground</h1>
            <p>Intuitive Scripting Language - Try it live!</p>
          </div>
        </div>
      </header>

      <div className="status-bar">
        {error && (
          <div className="status-message status-error">
            <span className="status-icon">❌</span>
            <span className="status-text">{error}</span>
          </div>
        )}
        {validationErrors.length > 0 && (
          <div className="status-message status-warning">
            <span className="status-icon">⚠️</span>
            <span className="status-text">
              {validationErrors.length} validation error{validationErrors.length > 1 ? 's' : ''}: {validationErrors[0]}
            </span>
          </div>
        )}
        {validationSuccess && !error && validationErrors.length === 0 && (
          <div className="status-message status-success">
            <span className="status-icon">✓</span>
            <span className="status-text">Code validated successfully</span>
          </div>
        )}
        {!error && !validationSuccess && validationErrors.length === 0 && output && (
          <div className="status-message status-success">
            <span className="status-icon">✓</span>
            <span className="status-text">Transformation completed successfully</span>
          </div>
        )}
        {!error && !validationSuccess && validationErrors.length === 0 && !output && (
          <div className="status-message status-ready">
            <span className="status-icon">✓</span>
            <span className="status-text">Ready to run</span>
          </div>
        )}
      </div>

      <div className="editor-container">
        <div className="editor-panel">
          <div className="panel-header">
            <h3>Input JSON</h3>
          </div>
          <div className="editor-wrapper">
            <Editor
              height="100%"
              language="json"
              theme="vs-dark"
              value={inputJson}
              onChange={(value) => setInputJson(value || '')}
              options={{
                minimap: { enabled: false },
                fontSize: 14,
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
              }}
            />
          </div>
        </div>

        <div className="editor-panel">
          <div className="panel-header">
            <h3>ISL Code</h3>
            <div className="button-group">
              <button
                onClick={handleValidate}
                disabled={loading}
                className="btn-validate"
                title="Validate ISL code (Ctrl+Q)"
              >
                {loading ? 'Validating...' : 'Validate (Ctrl+Q)'}
              </button>
              <button
                onClick={handleRun}
                disabled={loading}
                className="btn-run"
                title="Run transformation (Ctrl+R)"
              >
                {loading ? 'Running...' : 'Run (Ctrl+R)'}
              </button>
            </div>
          </div>
          <div className="editor-wrapper">
            <Editor
              height="100%"
              language="isl"
              theme="isl-dark"
              value={islCode}
              onChange={(value) => {
                setIslCode(value || '');
                clearErrorDecorations(); // Clear errors when user types
              }}
              onMount={(editor) => {
                editorRef.current = editor;
              }}
              beforeMount={(monaco) => {
                // Register ISL language before mounting the editor
                // @ts-ignore - Monaco types incompatibility
                registerIslLanguage(monaco);
              }}
              options={{
                minimap: { enabled: false },
                fontSize: 14,
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
              }}
            />
          </div>
        </div>

        <div className="editor-panel">
          <div className="panel-header">
            <h3>Output</h3>
          </div>
            <div className="editor-wrapper">
              <Editor
                height="100%"
                language="json"
                theme="vs-dark"
                value={output}
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  lineNumbers: 'on',
                  scrollBeyondLastLine: false,
                  automaticLayout: true,
                  readOnly: true,
                }}
              />
            </div>
        </div>
      </div>

      <footer className="footer">
        <p>
          Learn more about ISL:{' '}
          <a href="https://intuit.github.io/isl" target="_blank" rel="noopener noreferrer">
            Documentation
          </a>
        </p>
      </footer>
    </div>
  );
}

export default App;
