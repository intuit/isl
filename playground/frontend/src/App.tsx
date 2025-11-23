import { useState, useEffect, useCallback } from 'react';
import Editor from '@monaco-editor/react';
import axios from 'axios';
import './App.css';
import { registerIslLanguage } from './isl-language';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

interface Example {
  name: string;
  description: string;
  isl: string;
  input: string;
  expectedOutput: string;
}

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

function App() {
  const [islCode, setIslCode] = useState('fun run( $input )\n{\n    result: $input.message\n}');
  const [inputJson, setInputJson] = useState('{\n  "message": "Hello, ISL!"\n}');
  const [output, setOutput] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [examples, setExamples] = useState<Example[]>([]);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [validationSuccess, setValidationSuccess] = useState(false);

  const loadExamples = async () => {
    try {
      const response = await axios.get<Example[]>(`${API_BASE_URL}/examples`);
      setExamples(response.data);
    } catch (err) {
      console.error('Failed to load examples:', err);
    }
  };

  const handleRun = useCallback(async () => {
    if (loading) return;
    
    setLoading(true);
    setError('');
    setOutput('');
    setValidationErrors([]);
    setValidationSuccess(false);

    try {
      const response = await axios.post<TransformResponse>(`${API_BASE_URL}/transform`, {
        isl: islCode,
        input: inputJson,
      });

      if (response.data.success) {
        setOutput(response.data.output || '');
      } else if (response.data.error) {
        const err = response.data.error;
        const errorMsg = err.line && err.column
          ? `${err.type} at line ${err.line}, column ${err.column}: ${err.message}`
          : `${err.type}: ${err.message}`;
        setError(errorMsg);
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

    try {
      const response = await axios.post<ValidationResponse>(`${API_BASE_URL}/validate`, {
        isl: islCode,
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
      }
    } catch (err) {
      setError(`Validation request failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  }, [islCode, loading]);

  useEffect(() => {
    // Load examples
    loadExamples();
  }, []);

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

  const loadExample = (example: Example) => {
    setIslCode(example.isl);
    setInputJson(example.input);
    setOutput('');
    setError('');
    setValidationErrors([]);
    setValidationSuccess(false);
  };

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

      <div className="examples-bar">
        <label>Examples:</label>
        <select onChange={(e) => {
          const example = examples.find(ex => ex.name === e.target.value);
          if (example) loadExample(example);
        }}>
          <option value="">Select an example...</option>
          {examples.map((ex) => (
            <option key={ex.name} value={ex.name}>
              {ex.name}
            </option>
          ))}
        </select>
      </div>

      <div className="editor-container">
        <div className="editor-panel">
          <div className="panel-header">
            <h3>Input JSON</h3>
          </div>
          <Editor
            height="400px"
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
          <Editor
            height="400px"
            language="isl"
            theme="isl-dark"
            value={islCode}
            onChange={(value) => setIslCode(value || '')}
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
          {validationSuccess && (
            <div className="success-box">
              <h4>✓ Validation Successful!</h4>
              <p>ISL code is valid and ready to run.</p>
            </div>
          )}
          {validationErrors.length > 0 && (
            <div className="error-box validation-errors">
              <h4>Validation Errors:</h4>
              <ul>
                {validationErrors.map((err, idx) => (
                  <li key={idx}>{err}</li>
                ))}
              </ul>
            </div>
          )}
        </div>

        <div className="editor-panel">
          <div className="panel-header">
            <h3>Output</h3>
          </div>
          {error ? (
            <div className="error-box">
              <h4>Error:</h4>
              <pre>{error}</pre>
            </div>
          ) : (
            <Editor
              height="400px"
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
          )}
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
