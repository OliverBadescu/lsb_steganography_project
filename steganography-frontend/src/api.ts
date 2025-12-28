const API_BASE = 'http://localhost:8080/api/v1';

export interface DecodeResponse {
  message: string;
  messageLength: number;
  bitsPerChannelUsed: number;
}

export interface AnalysisResponse {
  mse: number;
  psnr: number;
  qualityAssessment: string;
  originalWidth: number;
  originalHeight: number;
}

export interface CapacityResponse {
  width: number;
  height: number;
  bitsPerChannel: number;
  capacityBytes: number;
  capacityKilobytes: number;
  totalPixels: number;
}

export async function encodeMessage(
  image: File,
  message: string,
  bitsPerChannel: number
): Promise<Blob> {
  const formData = new FormData();
  formData.append('image', image);
  formData.append('message', message);
  formData.append('bitsPerChannel', bitsPerChannel.toString());

  const response = await fetch(`${API_BASE}/encode`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to encode message');
  }

  return response.blob();
}

export async function decodeMessage(
  image: File,
  bitsPerChannel: number
): Promise<DecodeResponse> {
  const formData = new FormData();
  formData.append('image', image);
  formData.append('bitsPerChannel', bitsPerChannel.toString());

  const response = await fetch(`${API_BASE}/decode`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to decode message');
  }

  return response.json();
}

export async function analyzeImages(
  original: File,
  stego: File
): Promise<AnalysisResponse> {
  const formData = new FormData();
  formData.append('original', original);
  formData.append('stego', stego);

  const response = await fetch(`${API_BASE}/analyze`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to analyze images');
  }

  return response.json();
}

export async function getCapacity(
  width: number,
  height: number,
  bitsPerChannel: number
): Promise<CapacityResponse> {
  const params = new URLSearchParams({
    width: width.toString(),
    height: height.toString(),
    bitsPerChannel: bitsPerChannel.toString(),
  });

  const response = await fetch(`${API_BASE}/capacity?${params}`);

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to get capacity');
  }

  return response.json();
}
