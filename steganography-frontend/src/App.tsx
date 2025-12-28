import { useState } from 'react';
import {
  encodeMessage,
  decodeMessage,
  analyzeImages,
  getCapacity,
  type DecodeResponse,
  type AnalysisResponse,
  type CapacityResponse,
} from './api';
import './App.css';

type Tab = 'encode' | 'decode' | 'analyze' | 'capacity';

function App() {
  const [activeTab, setActiveTab] = useState<Tab>('encode');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Encode state
  const [encodeImage, setEncodeImage] = useState<File | null>(null);
  const [messageText, setMessageText] = useState('');
  const [encodeBits, setEncodeBits] = useState(1);
  const [encodePreview, setEncodePreview] = useState<string | null>(null);

  // Decode state
  const [decodeImage, setDecodeImage] = useState<File | null>(null);
  const [decodeBits, setDecodeBits] = useState(1);
  const [decodeResult, setDecodeResult] = useState<DecodeResponse | null>(null);

  // Analyze state
  const [originalImage, setOriginalImage] = useState<File | null>(null);
  const [stegoImage, setStegoImage] = useState<File | null>(null);
  const [analysisResult, setAnalysisResult] = useState<AnalysisResponse | null>(null);

  // Capacity state
  const [capWidth, setCapWidth] = useState(800);
  const [capHeight, setCapHeight] = useState(600);
  const [capBits, setCapBits] = useState(1);
  const [capacityResult, setCapacityResult] = useState<CapacityResponse | null>(null);

  const handleEncode = async () => {
    if (!encodeImage || !messageText) {
      setError('Please select an image and enter a message');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const blob = await encodeMessage(encodeImage, messageText, encodeBits);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `stego_${encodeBits}bpc.png`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Encoding failed');
    } finally {
      setLoading(false);
    }
  };

  const handleDecode = async () => {
    if (!decodeImage) {
      setError('Please select an image');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const result = await decodeMessage(decodeImage, decodeBits);
      setDecodeResult(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Decoding failed');
    } finally {
      setLoading(false);
    }
  };

  const handleAnalyze = async () => {
    if (!originalImage || !stegoImage) {
      setError('Please select both images');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const result = await analyzeImages(originalImage, stegoImage);
      setAnalysisResult(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Analysis failed');
    } finally {
      setLoading(false);
    }
  };

  const handleCapacity = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await getCapacity(capWidth, capHeight, capBits);
      setCapacityResult(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to calculate capacity');
    } finally {
      setLoading(false);
    }
  };

  const handleImageSelect = (
    e: React.ChangeEvent<HTMLInputElement>,
    setter: (f: File | null) => void,
    previewSetter?: (s: string | null) => void
  ) => {
    const file = e.target.files?.[0] || null;
    setter(file);
    if (previewSetter && file) {
      const reader = new FileReader();
      reader.onload = () => previewSetter(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  return (
    <div className="container">
      <h1>LSB Steganography</h1>
      <p className="subtitle">Hide messages in images using Least Significant Bit technique</p>

      <nav className="tabs">
        {(['encode', 'decode', 'analyze', 'capacity'] as Tab[]).map((tab) => (
          <button
            key={tab}
            className={activeTab === tab ? 'active' : ''}
            onClick={() => {
              setActiveTab(tab);
              setError(null);
            }}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </nav>

      {error && <div className="error">{error}</div>}

      <div className="panel">
        {activeTab === 'encode' && (
          <>
            <h2>Hide Message in Image</h2>
            <div className="field">
              <label>Cover Image (PNG)</label>
              <input
                type="file"
                accept="image/png"
                onChange={(e) => handleImageSelect(e, setEncodeImage, setEncodePreview)}
              />
              {encodePreview && <img src={encodePreview} alt="Preview" className="preview" />}
            </div>
            <div className="field">
              <label>Secret Message</label>
              <textarea
                value={messageText}
                onChange={(e) => setMessageText(e.target.value)}
                placeholder="Enter your secret message..."
                rows={4}
              />
            </div>
            <div className="field">
              <label>Bits per Channel: {encodeBits}</label>
              <input
                type="range"
                min="1"
                max="8"
                value={encodeBits}
                onChange={(e) => setEncodeBits(Number(e.target.value))}
              />
              <span className="hint">
                {encodeBits === 1 && 'Most secure, lowest capacity'}
                {encodeBits >= 2 && encodeBits <= 3 && 'Good balance'}
                {encodeBits >= 4 && encodeBits <= 6 && 'Higher capacity, more visible'}
                {encodeBits >= 7 && 'Maximum capacity, easily detectable'}
              </span>
            </div>
            <button className="primary" onClick={handleEncode} disabled={loading}>
              {loading ? 'Encoding...' : 'Encode & Download'}
            </button>
          </>
        )}

        {activeTab === 'decode' && (
          <>
            <h2>Extract Hidden Message</h2>
            <div className="field">
              <label>Stego Image (PNG)</label>
              <input
                type="file"
                accept="image/png"
                onChange={(e) => handleImageSelect(e, setDecodeImage)}
              />
            </div>
            <div className="field">
              <label>Bits per Channel: {decodeBits}</label>
              <input
                type="range"
                min="1"
                max="8"
                value={decodeBits}
                onChange={(e) => setDecodeBits(Number(e.target.value))}
              />
            </div>
            <button className="primary" onClick={handleDecode} disabled={loading}>
              {loading ? 'Decoding...' : 'Decode Message'}
            </button>
            {decodeResult && (
              <div className="result">
                <h3>Extracted Message</h3>
                <div className="message-box">{decodeResult.message}</div>
                <p className="meta">
                  Length: {decodeResult.messageLength} characters | Bits used: {decodeResult.bitsPerChannelUsed}
                </p>
              </div>
            )}
          </>
        )}

        {activeTab === 'analyze' && (
          <>
            <h2>Compare Images</h2>
            <div className="field-row">
              <div className="field">
                <label>Original Image</label>
                <input
                  type="file"
                  accept="image/png"
                  onChange={(e) => handleImageSelect(e, setOriginalImage)}
                />
              </div>
              <div className="field">
                <label>Stego Image</label>
                <input
                  type="file"
                  accept="image/png"
                  onChange={(e) => handleImageSelect(e, setStegoImage)}
                />
              </div>
            </div>
            <button className="primary" onClick={handleAnalyze} disabled={loading}>
              {loading ? 'Analyzing...' : 'Analyze Quality'}
            </button>
            {analysisResult && (
              <div className="result">
                <h3>Quality Metrics</h3>
                <div className="metrics">
                  <div className="metric">
                    <span className="value">{analysisResult.psnr.toFixed(2)} dB</span>
                    <span className="label">PSNR</span>
                  </div>
                  <div className="metric">
                    <span className="value">{analysisResult.mse.toFixed(6)}</span>
                    <span className="label">MSE</span>
                  </div>
                  <div className="metric">
                    <span className={`value quality-${analysisResult.qualityAssessment.toLowerCase().replace(' ', '-')}`}>
                      {analysisResult.qualityAssessment}
                    </span>
                    <span className="label">Quality</span>
                  </div>
                </div>
                <p className="meta">
                  Image size: {analysisResult.originalWidth} x {analysisResult.originalHeight}
                </p>
              </div>
            )}
          </>
        )}

        {activeTab === 'capacity' && (
          <>
            <h2>Calculate Capacity</h2>
            <div className="field-row">
              <div className="field">
                <label>Width (px)</label>
                <input
                  type="number"
                  value={capWidth}
                  onChange={(e) => setCapWidth(Number(e.target.value))}
                  min="1"
                />
              </div>
              <div className="field">
                <label>Height (px)</label>
                <input
                  type="number"
                  value={capHeight}
                  onChange={(e) => setCapHeight(Number(e.target.value))}
                  min="1"
                />
              </div>
            </div>
            <div className="field">
              <label>Bits per Channel: {capBits}</label>
              <input
                type="range"
                min="1"
                max="8"
                value={capBits}
                onChange={(e) => setCapBits(Number(e.target.value))}
              />
            </div>
            <button className="primary" onClick={handleCapacity} disabled={loading}>
              {loading ? 'Calculating...' : 'Calculate'}
            </button>
            {capacityResult && (
              <div className="result">
                <h3>Storage Capacity</h3>
                <div className="metrics">
                  <div className="metric">
                    <span className="value">{capacityResult.capacityKilobytes.toFixed(2)} KB</span>
                    <span className="label">Capacity</span>
                  </div>
                  <div className="metric">
                    <span className="value">{capacityResult.capacityBytes.toLocaleString()}</span>
                    <span className="label">Bytes</span>
                  </div>
                  <div className="metric">
                    <span className="value">{capacityResult.totalPixels.toLocaleString()}</span>
                    <span className="label">Total Pixels</span>
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default App;
