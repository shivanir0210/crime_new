import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import Panel from '../components/Panel';
import { 
  getCaseById, 
  updateCase, 
  updateCaseStatus, 
  updateCasePriority,
  getCaseFIR,
  createCaseFIR,
  updateCaseFIR,
  getCaseVictims,
  createCaseVictim,
  updateCaseVictim,
  getCaseSuspects,
  createCaseSuspect,
  updateCaseSuspect,
  getCaseWitnesses,
  createCaseWitness,
  updateCaseWitness,
  deleteCaseWitness,
  getCaseTimeline,
  getCaseEvidence,
  getEvidenceDetail,
  uploadEvidence,
  updateEvidence,
  updateEvidenceStatus,
  downloadEvidenceFile,
  getEvidenceAuditLogs,
  getCaseState,
  getEvidenceGaps,
  analyzeEvidenceGaps,
  updateEvidenceGapStatus,
  getNotifications,
  sendTestCaseNotification,
  getCaseOfficers,
  getAvailableOfficers,
  assignCaseOfficer,
  removeCaseOfficer,
  searchCasePublicInformation,
  getCaseSuggestedQueries,
  savePublicInformation,
  getCasePublicInformation,
  updatePublicInformationItem,
  downloadCaseReportPdf,
  getCaseReportHistory,
  generateCaseIllustration,
  generateEvidenceIllustration,
  getCaseIllustrations,
  deleteCaseIllustration,
  uploadSuspectPhoto,
  getSuspectPhotoUrl,
  getSuspectDefaultImageUrl,
  uploadVictimPhoto,
  getVictimPhotoUrl,
  getVictimDefaultImageUrl,
  getEvidenceViewUrl,
  getIllustrationViewUrl,
  resolveAssetUrl,
  replaceEvidenceFile,
  API_BASE_URL
} from '../services/api';
import InvestigationStateWidget from '../components/InvestigationStateWidget';
import EvidenceGapDashboard from '../components/EvidenceGapDashboard';
import AIInvestigationPanel from '../components/AIInvestigationPanel';
import WhatsAppPreviewModal from '../components/WhatsAppPreviewModal';
import CaseHeader from '../components/CaseHeader';
import CaseCommandCenter from '../components/CaseCommandCenter';
import InvestigationRelationshipView from '../components/InvestigationRelationshipView';
import CaseTimelineTab from '../components/CaseTimelineTab';
import WitnessManagementTab from '../components/WitnessManagementTab';
import InterStationCoordinationModal from '../components/InterStationCoordinationModal';
import { 
  ArrowLeft, 
  ShieldAlert, 
  Clock, 
  UserCheck, 
  Building2, 
  Calendar, 
  Tag, 
  Edit3, 
  CheckCircle, 
  AlertTriangle,
  FolderLock,
  Lock,
  FileText,
  UserX,
  Users,
  Eye,
  Cpu,
  Save,
  X,
  PlusCircle,
  FilePlus2,
  FileBadge,
  Phone,
  MapPin,
  Briefcase,
  AlertCircle,
  UploadCloud,
  Download,
  FileCheck,
  HardDrive,
  Hash,
  Copy,
  Layers,
  FileCode,
  Image as ImageIcon,
  Video,
  Music,
  FileSpreadsheet,
  Bell,
  Send,
  Mail,
  MessageSquare,
  RefreshCw,
  Globe,
  ExternalLink,
  BookmarkPlus,
  BookmarkCheck,
  Trash2,
  Search,
  Info,
  History,
  Sparkles,
  Camera,
  Maximize2,
  Check,
  Wand2
} from 'lucide-react';

const isMockPublicUrl = (url, sourceName = '', title = '', provider = '') => {
  if (!url) return true;
  const u = String(url).toLowerCase();
  const s = String(sourceName).toLowerCase();
  const t = String(title).toLowerCase();
  const p = String(provider).toLowerCase();

  return (
    u.includes('.internal') ||
    u.includes('/mock-') ||
    u.includes('/simulated-') ||
    s.includes('[mock]') ||
    t.includes('[development mock data]') ||
    p === 'mock'
  );
};

const isPlayableVideoUrl = (url) => {
  if (!url || typeof url !== 'string') return false;
  const clean = url.split('?')[0].toLowerCase();
  return clean.endsWith('.mp4') || clean.endsWith('.webm') || clean.endsWith('.ogg') || clean.endsWith('.m4v');
};

const renderDefaultEvidenceSvg = (evidenceType, title, description, evidenceNumber) => {
  const normType = (evidenceType || 'OTHER').toUpperCase();
  const safeTitle = String(title || 'Forensic Specimen').replace(/[<>&"]/g, '');
  const safeNum = String(evidenceNumber || 'EVD-SPECIMEN').replace(/[<>&"]/g, '');

  if (normType === 'CCTV') {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="100%" height="100%" style="display:block; background:#060a12; font-family:'JetBrains Mono', monospace;">
      <defs>
        <radialGradient id="cctvGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#0e2a3b" stop-opacity="0.8"/>
          <stop offset="100%" stop-color="#060a12" stop-opacity="1"/>
        </radialGradient>
      </defs>
      <rect width="400" height="260" fill="url(#cctvGlow)"/>
      <path d="M0 65 H400 M0 130 H400 M0 195 H400 M100 0 V260 M200 0 V260 M300 0 V260" stroke="rgba(34,211,238,0.08)" stroke-width="1"/>
      <path d="M20 35 H45 M20 35 V60 M380 35 H355 M380 35 V60 M20 225 H45 M20 225 V200 M380 225 H355 M380 225 V200" stroke="#06b6d4" stroke-width="2" fill="none"/>
      <circle cx="200" cy="130" r="42" stroke="rgba(34,211,238,0.3)" stroke-width="1.5" stroke-dasharray="4 3" fill="none"/>
      <circle cx="200" cy="130" r="12" stroke="#22d3ee" stroke-width="1.5" fill="none"/>
      <line x1="140" y1="130" x2="260" y2="130" stroke="rgba(34,211,238,0.4)" stroke-width="1"/>
      <line x1="200" y1="70" x2="200" y2="190" stroke="rgba(34,211,238,0.4)" stroke-width="1"/>
      <rect x="25" y="42" width="10" height="10" rx="5" fill="#ef4444"/>
      <text x="42" y="51" fill="#ef4444" font-size="10" font-weight="700" letter-spacing="1">LIVE REC • CAM-02</text>
      <text x="375" y="51" text-anchor="end" fill="#94a3b8" font-size="9" letter-spacing="0.5">02:41:19 AM UTC</text>
      <rect x="25" y="200" width="350" height="32" rx="3" fill="rgba(15,23,42,0.85)" stroke="rgba(34,211,238,0.3)" stroke-width="1"/>
      <text x="35" y="214" fill="#38bdf8" font-size="10" font-weight="700">CCTV SURVEILLANCE FEED</text>
      <text x="35" y="226" fill="#94a3b8" font-size="8.5">${safeTitle.length > 40 ? safeTitle.substring(0, 38) + '...' : safeTitle}</text>
      <text x="365" y="220" text-anchor="end" fill="#22d3ee" font-size="9" font-weight="700">[PERIMETER VIEW]</text>
    </svg>`;
  }

  if (normType === 'VIDEO') {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="100%" height="100%" style="display:block; background:#080a14; font-family:'JetBrains Mono', monospace;">
      <defs>
        <radialGradient id="vidGlow" cx="50%" cy="50%" r="55%">
          <stop offset="0%" stop-color="#141a33" stop-opacity="0.9"/>
          <stop offset="100%" stop-color="#080a14" stop-opacity="1"/>
        </radialGradient>
      </defs>
      <rect width="400" height="260" fill="url(#vidGlow)"/>
      <path d="M0 50 H400 M0 210 H400" stroke="rgba(129,140,248,0.15)" stroke-width="1"/>
      <rect x="30" y="45" width="340" height="155" rx="4" fill="rgba(15,23,42,0.6)" stroke="rgba(99,102,241,0.4)" stroke-width="1.5"/>
      <rect x="130" y="80" width="140" height="85" rx="2" fill="none" stroke="#818cf8" stroke-width="1.5" stroke-dasharray="3 3"/>
      <text x="135" y="93" fill="#818cf8" font-size="8.5" font-weight="700">MOTION DETECT: 96.4%</text>
      <circle cx="200" cy="125" r="24" fill="rgba(99,102,241,0.2)" stroke="#818cf8" stroke-width="1.5"/>
      <polygon points="193,114 213,125 193,136" fill="#c7d2fe"/>
      <rect x="40" y="180" width="320" height="4" rx="2" fill="rgba(255,255,255,0.1)"/>
      <rect x="40" y="180" width="180" height="4" rx="2" fill="#818cf8"/>
      <circle cx="220" cy="182" r="5" fill="#c7d2fe"/>
      <text x="35" y="32" fill="#a5b4fc" font-size="10" font-weight="700" letter-spacing="0.5">VIDEO FORENSIC EXTRACTION</text>
      <text x="365" y="32" text-anchor="end" fill="#64748b" font-size="9">60 FPS • 1080P</text>
      <text x="40" y="228" fill="#e0e7ff" font-size="9.5" font-weight="700">${safeTitle.length > 40 ? safeTitle.substring(0, 38) + '...' : safeTitle}</text>
      <text x="40" y="242" fill="#94a3b8" font-size="8.5">TRACK ID: ${safeNum} • OPTICAL FLOW ANALYSIS</text>
    </svg>`;
  }

  if (normType === 'DOCUMENT' || normType === 'FORENSIC_REPORT') {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="100%" height="100%" style="display:block; background:#0a0d16; font-family:'JetBrains Mono', monospace;">
      <defs>
        <linearGradient id="docBg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#111827"/>
          <stop offset="100%" stop-color="#0a0d16"/>
        </linearGradient>
      </defs>
      <rect width="400" height="260" fill="url(#docBg)"/>
      <rect x="65" y="25" width="270" height="210" rx="4" fill="#0f172a" stroke="rgba(148,163,184,0.2)" stroke-width="1.5"/>
      <rect x="65" y="25" width="270" height="34" rx="4" fill="#1e293b" stroke="rgba(148,163,184,0.2)" stroke-width="1"/>
      <rect x="80" y="37" width="12" height="12" rx="2" fill="#38bdf8"/>
      <text x="100" y="47" fill="#f1f5f9" font-size="10" font-weight="800" letter-spacing="0.5">FORENSIC EXAMINATION DOCKET</text>
      <line x1="85" y1="75" x2="260" y2="75" stroke="#38bdf8" stroke-width="2.5"/>
      <line x1="85" y1="92" x2="315" y2="92" stroke="rgba(255,255,255,0.12)" stroke-width="2"/>
      <line x1="85" y1="108" x2="295" y2="108" stroke="rgba(255,255,255,0.12)" stroke-width="2"/>
      <line x1="85" y1="124" x2="310" y2="124" stroke="rgba(255,255,255,0.12)" stroke-width="2"/>
      <line x1="85" y1="140" x2="250" y2="140" stroke="rgba(255,255,255,0.12)" stroke-width="2"/>
      <g transform="translate(85, 160)">
        <rect x="0" y="0" width="3" height="30" fill="#94a3b8"/>
        <rect x="5" y="0" width="1.5" height="30" fill="#94a3b8"/>
        <rect x="9" y="0" width="4" height="30" fill="#94a3b8"/>
        <rect x="15" y="0" width="2" height="30" fill="#94a3b8"/>
        <rect x="19" y="0" width="5" height="30" fill="#94a3b8"/>
        <rect x="26" y="0" width="1.5" height="30" fill="#94a3b8"/>
        <rect x="30" y="0" width="3" height="30" fill="#94a3b8"/>
        <rect x="35" y="0" width="4" height="30" fill="#94a3b8"/>
        <text x="0" y="42" fill="#64748b" font-size="7">${safeNum}</text>
      </g>
      <g transform="translate(245, 145) rotate(-12)">
        <rect x="0" y="0" width="75" height="35" rx="3" fill="none" stroke="#ef4444" stroke-width="2" stroke-dasharray="4 2"/>
        <text x="37" y="15" text-anchor="middle" fill="#ef4444" font-size="8" font-weight="900" letter-spacing="1">LAB TESTED</text>
        <text x="37" y="27" text-anchor="middle" fill="#f87171" font-size="6.5" font-weight="700">SPECIMEN DOCKET</text>
      </g>
      <text x="65" y="248" fill="#94a3b8" font-size="8">${safeTitle.length > 38 ? safeTitle.substring(0, 36) + '...' : safeTitle}</text>
    </svg>`;
  }

  if (normType === 'AUDIO' || normType === 'CALL_RECORD') {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="100%" height="100%" style="display:block; background:#070b14; font-family:'JetBrains Mono', monospace;">
      <defs>
        <linearGradient id="audGrad" x1="0" y1="1" x2="0" y2="0">
          <stop offset="0%" stop-color="#059669"/>
          <stop offset="50%" stop-color="#10b981"/>
          <stop offset="100%" stop-color="#34d399"/>
        </linearGradient>
      </defs>
      <rect width="400" height="260" fill="#070b14"/>
      <path d="M20 130 H380" stroke="rgba(16,185,129,0.2)" stroke-width="1"/>
      <path d="M20 70 H380 M20 190 H380" stroke="rgba(255,255,255,0.05)" stroke-width="1" stroke-dasharray="2 4"/>
      <g transform="translate(30, 0)">
        <rect x="0" y="115" width="4" height="30" rx="2" fill="url(#audGrad)"/>
        <rect x="8" y="105" width="4" height="50" rx="2" fill="url(#audGrad)"/>
        <rect x="16" y="90" width="4" height="80" rx="2" fill="url(#audGrad)"/>
        <rect x="24" y="120" width="4" height="20" rx="2" fill="url(#audGrad)"/>
        <rect x="32" y="80" width="4" height="100" rx="2" fill="url(#audGrad)"/>
        <rect x="40" y="65" width="4" height="130" rx="2" fill="url(#audGrad)"/>
        <rect x="48" y="95" width="4" height="70" rx="2" fill="url(#audGrad)"/>
        <rect x="56" y="110" width="4" height="40" rx="2" fill="url(#audGrad)"/>
        <rect x="64" y="75" width="4" height="110" rx="2" fill="url(#audGrad)"/>
        <rect x="72" y="55" width="4" height="150" rx="2" fill="url(#audGrad)"/>
        <rect x="80" y="85" width="4" height="90" rx="2" fill="url(#audGrad)"/>
        <rect x="88" y="115" width="4" height="30" rx="2" fill="url(#audGrad)"/>
        <rect x="96" y="100" width="4" height="60" rx="2" fill="url(#audGrad)"/>
        <rect x="104" y="70" width="4" height="120" rx="2" fill="url(#audGrad)"/>
        <rect x="112" y="60" width="4" height="140" rx="2" fill="url(#audGrad)"/>
        <rect x="120" y="90" width="4" height="80" rx="2" fill="url(#audGrad)"/>
        <rect x="128" y="115" width="4" height="30" rx="2" fill="url(#audGrad)"/>
        <rect x="136" y="105" width="4" height="50" rx="2" fill="url(#audGrad)"/>
        <rect x="144" y="75" width="4" height="110" rx="2" fill="url(#audGrad)"/>
        <rect x="152" y="50" width="4" height="160" rx="2" fill="url(#audGrad)"/>
        <rect x="160" y="85" width="4" height="90" rx="2" fill="url(#audGrad)"/>
        <rect x="168" y="120" width="4" height="20" rx="2" fill="url(#audGrad)"/>
        <rect x="176" y="95" width="4" height="70" rx="2" fill="url(#audGrad)"/>
        <rect x="184" y="65" width="4" height="130" rx="2" fill="url(#audGrad)"/>
        <rect x="192" y="80" width="4" height="100" rx="2" fill="url(#audGrad)"/>
        <rect x="200" y="110" width="4" height="40" rx="2" fill="url(#audGrad)"/>
        <rect x="208" y="70" width="4" height="120" rx="2" fill="url(#audGrad)"/>
        <rect x="216" y="55" width="4" height="150" rx="2" fill="url(#audGrad)"/>
        <rect x="224" y="90" width="4" height="80" rx="2" fill="url(#audGrad)"/>
        <rect x="232" y="115" width="4" height="30" rx="2" fill="url(#audGrad)"/>
        <rect x="240" y="100" width="4" height="60" rx="2" fill="url(#audGrad)"/>
        <rect x="248" y="65" width="4" height="130" rx="2" fill="url(#audGrad)"/>
        <rect x="256" y="85" width="4" height="90" rx="2" fill="url(#audGrad)"/>
        <rect x="264" y="115" width="4" height="30" rx="2" fill="url(#audGrad)"/>
        <rect x="272" y="105" width="4" height="50" rx="2" fill="url(#audGrad)"/>
        <rect x="280" y="80" width="4" height="100" rx="2" fill="url(#audGrad)"/>
        <rect x="288" y="60" width="4" height="140" rx="2" fill="url(#audGrad)"/>
        <rect x="296" y="95" width="4" height="70" rx="2" fill="url(#audGrad)"/>
        <rect x="304" y="115" width="4" height="30" rx="2" fill="url(#audGrad)"/>
        <rect x="312" y="122" width="4" height="16" rx="2" fill="url(#audGrad)"/>
        <rect x="320" y="125" width="4" height="10" rx="2" fill="url(#audGrad)"/>
      </g>
      <text x="25" y="32" fill="#34d399" font-size="10" font-weight="700" letter-spacing="0.5">ACOUSTIC & CDR TELEMETRY</text>
      <text x="375" y="32" text-anchor="end" fill="#64748b" font-size="9">48 kHz • 24-bit PCM</text>
      <text x="25" y="234" fill="#ecfdf5" font-size="9.5" font-weight="700">${safeTitle.length > 40 ? safeTitle.substring(0, 38) + '...' : safeTitle}</text>
      <text x="25" y="248" fill="#6ee7b7" font-size="8.5">VOICEPRINT SPECTRUM & CALL LOG ANALYSIS</text>
    </svg>`;
  }

  if (normType === 'DIGITAL_DEVICE') {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="100%" height="100%" style="display:block; background:#090d18; font-family:'JetBrains Mono', monospace;">
      <defs>
        <linearGradient id="digiGrad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#1e1b4b"/>
          <stop offset="100%" stop-color="#0f172a"/>
        </linearGradient>
      </defs>
      <rect width="400" height="260" fill="url(#digiGrad)"/>
      <path d="M40 70 H120 V110 H180 M220 150 H280 V190 H360 M40 190 H100 V150 H160" stroke="rgba(168,85,247,0.25)" stroke-width="1.5" fill="none"/>
      <circle cx="120" cy="110" r="3" fill="#c084fc"/>
      <circle cx="180" cy="110" r="3" fill="#c084fc"/>
      <circle cx="220" cy="150" r="3" fill="#c084fc"/>
      <circle cx="280" cy="190" r="3" fill="#c084fc"/>
      <rect x="140" y="55" width="120" height="145" rx="8" fill="#030712" stroke="#a855f7" stroke-width="2"/>
      <rect x="150" y="70" width="100" height="105" rx="3" fill="#0f172a"/>
      <text x="156" y="85" fill="#a855f7" font-size="7" font-weight="700">EXTRACTING...</text>
      <rect x="156" y="92" width="88" height="4" rx="2" fill="rgba(255,255,255,0.1)"/>
      <rect x="156" y="92" width="62" height="4" rx="2" fill="#c084fc"/>
      <text x="156" y="110" fill="#e2e8f0" font-size="6.5">0x4A 0x69 0x20</text>
      <text x="156" y="122" fill="#e2e8f0" font-size="6.5">0xFF 0x00 0xBC</text>
      <text x="156" y="134" fill="#e2e8f0" font-size="6.5">NAND FLASH DUMP</text>
      <circle cx="200" cy="187" r="4" fill="rgba(255,255,255,0.2)"/>
      <text x="25" y="32" fill="#c084fc" font-size="10" font-weight="700" letter-spacing="0.5">DIGITAL HARDWARE FORENSICS</text>
      <text x="375" y="32" text-anchor="end" fill="#64748b" font-size="9">CHIP-OFF MEMORY EXTRACTION</text>
      <text x="25" y="234" fill="#f3e8ff" font-size="9.5" font-weight="700">${safeTitle.length > 40 ? safeTitle.substring(0, 38) + '...' : safeTitle}</text>
      <text x="25" y="248" fill="#d8b4fe" font-size="8.5">HARDWARE IDENTIFIER: ${safeNum}</text>
    </svg>`;
  }

  if (normType === 'IMAGE') {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="100%" height="100%" style="display:block; background:#0a0e17; font-family:'JetBrains Mono', monospace;">
      <defs>
        <radialGradient id="imgGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#162e3b"/>
          <stop offset="100%" stop-color="#0a0e17"/>
        </radialGradient>
      </defs>
      <rect width="400" height="260" fill="url(#imgGlow)"/>
      <rect x="40" y="45" width="320" height="155" rx="4" fill="#0f172a" stroke="rgba(56,189,248,0.3)" stroke-width="1.5"/>
      <circle cx="200" cy="122" r="38" stroke="#38bdf8" stroke-width="1.5" stroke-dasharray="3 3" fill="none"/>
      <path d="M185 122 H215 M200 107 V137" stroke="#38bdf8" stroke-width="1.5"/>
      <path d="M55 60 H70 V75 M345 60 H330 V75 M55 185 H70 V170 M345 185 H330 V170" stroke="#0ea5e9" stroke-width="2" fill="none"/>
      <text x="25" y="32" fill="#38bdf8" font-size="10" font-weight="700" letter-spacing="0.5">IMAGE METADATA & SPECTRAL ANALYSIS</text>
      <text x="375" y="32" text-anchor="end" fill="#64748b" font-size="9">EXIF VERIFIED</text>
      <text x="40" y="228" fill="#f0f9ff" font-size="9.5" font-weight="700">${safeTitle.length > 40 ? safeTitle.substring(0, 38) + '...' : safeTitle}</text>
      <text x="40" y="242" fill="#7dd3fc" font-size="8.5">PIXEL RESOLUTION: 4032x3024 • SPECIMEN #${safeNum}</text>
    </svg>`;
  }

  // OTHER / GENERAL FORENSIC SPECIMEN
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 260" width="100%" height="100%" style="display:block; background:#0d1117; font-family:'JetBrains Mono', monospace;">
    <defs>
      <linearGradient id="otherBg" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#161b22"/>
        <stop offset="100%" stop-color="#0d1117"/>
      </linearGradient>
    </defs>
    <rect width="400" height="260" fill="url(#otherBg)"/>
    <path d="M0 65 H400 M0 195 H400 M100 0 V260 M300 0 V260" stroke="rgba(255,255,255,0.04)" stroke-width="1"/>
    <rect x="75" y="45" width="250" height="155" rx="6" fill="#161e2e" stroke="rgba(245,158,11,0.35)" stroke-width="1.5"/>
    <rect x="75" y="45" width="250" height="28" rx="6" fill="#1e293b" stroke="rgba(245,158,11,0.35)" stroke-width="1"/>
    <circle cx="92" cy="59" r="5" fill="#f59e0b"/>
    <text x="105" y="63" fill="#fbbf24" font-size="9.5" font-weight="800">EVIDENCE SPECIMEN REPOSITORY</text>
    <text x="95" y="95" fill="#94a3b8" font-size="8.5" font-weight="700">CHAIN OF CUSTODY SPECIMEN:</text>
    <text x="95" y="112" fill="#f8fafc" font-size="10" font-weight="800">${safeTitle.length > 28 ? safeTitle.substring(0, 26) + '...' : safeTitle}</text>
    <text x="95" y="130" fill="#f59e0b" font-size="8.5">ITEM REF: ${safeNum}</text>
    <polygon points="280,110 260,170 300,170" fill="rgba(245,158,11,0.15)" stroke="#f59e0b" stroke-width="1.5"/>
    <text x="280" y="152" text-anchor="middle" fill="#fbbf24" font-size="14" font-weight="900">#01</text>
    <text x="25" y="30" fill="#fbbf24" font-size="10" font-weight="700">PHYSICAL FORENSIC EVIDENCE</text>
    <text x="375" y="30" text-anchor="end" fill="#64748b" font-size="9">SECURE VAULT</text>
    <text x="25" y="235" fill="#f1f5f9" font-size="9.5" font-weight="700">${safeTitle.length > 40 ? safeTitle.substring(0, 38) + '...' : safeTitle}</text>
    <text x="25" y="248" fill="#fde68a" font-size="8.5">TYPE: ${normType.replace(/_/g, ' ')} • PENDING PHYSICAL FILE</text>
  </svg>`;
};

export default function CaseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [activeTab, setActiveTabState] = useState(searchParams.get('tab') || 'OVERVIEW');
  const setActiveTab = useCallback((tab) => {
    setActiveTabState(tab);
    setSearchParams({ tab });
  }, [setSearchParams]);

  useEffect(() => {
    const tabFromUrl = searchParams.get('tab');
    if (tabFromUrl && tabFromUrl !== activeTab) {
      setActiveTabState(tabFromUrl);
    }
  }, [searchParams]);

  const [caseData, setCaseData] = useState(null);
  const [firData, setFirData] = useState(null);
  const [victims, setVictims] = useState([]);
  const [suspects, setSuspects] = useState([]);
  const [witnesses, setWitnesses] = useState([]);
  const [loadingWitnesses, setLoadingWitnesses] = useState(false);
  const [timelineEvents, setTimelineEvents] = useState([]);
  const [loadingTimeline, setLoadingTimeline] = useState(false);
  const [evidenceList, setEvidenceList] = useState([]);
  const [caseState, setCaseState] = useState(null);
  const [evidenceGaps, setEvidenceGaps] = useState([]);
  const [assignedOfficers, setAssignedOfficers] = useState([]);
  const [reportHistory, setReportHistory] = useState([]);
  const [loadingReportHistory, setLoadingReportHistory] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const showFlash = useCallback((msg) => {
    setSuccessMessage(msg);
    setTimeout(() => setSuccessMessage(''), 4000);
  }, []);

  // Edit Case Modal state
  const [isEditCaseModalOpen, setIsEditCaseModalOpen] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editType, setEditType] = useState('MURDER');
  const [editPriority, setEditPriority] = useState('HIGH');
  const [editStatus, setEditStatus] = useState('OPEN');
  const [savingCase, setSavingCase] = useState(false);

  // FIR Modal State
  const [isFirModalOpen, setIsFirModalOpen] = useState(false);
  const [isEditingFir, setIsEditingFir] = useState(false);
  const [firComplainant, setFirComplainant] = useState('');
  const [firContact, setFirContact] = useState('');
  const [firRegDate, setFirRegDate] = useState('');
  const [firIncDate, setFirIncDate] = useState('');
  const [firLocation, setFirLocation] = useState('');
  const [firOffenceType, setFirOffenceType] = useState('');
  const [firSections, setFirSections] = useState('');
  const [firDesc, setFirDesc] = useState('');
  const [savingFir, setSavingFir] = useState(false);

  // Victim Modal State
  const [isVictimModalOpen, setIsVictimModalOpen] = useState(false);
  const [editingVictimId, setEditingVictimId] = useState(null);
  const [vFullName, setVFullName] = useState('');
  const [vAge, setVAge] = useState('');
  const [vGender, setVGender] = useState('MALE');
  const [vDob, setVDob] = useState('');
  const [vPhone, setVPhone] = useState('');
  const [vAddress, setVAddress] = useState('');
  const [vOccupation, setVOccupation] = useState('');
  const [vRelationship, setVRelationship] = useState('Primary Victim');
  const [vDescription, setVDescription] = useState('');
  const [vStatus, setVStatus] = useState('ACTIVE');
  const [savingVictim, setSavingVictim] = useState(false);

  // Inter-Station Coordination Modal State
  const [isCoordinationModalOpen, setIsCoordinationModalOpen] = useState(false);
  const [selectedCoordinationVictim, setSelectedCoordinationVictim] = useState(null);

  const handleOpenCoordinationModal = (victim) => {
    setSelectedCoordinationVictim(victim);
    setIsCoordinationModalOpen(true);
  };

  // Suspect Modal State
  const [isSuspectModalOpen, setIsSuspectModalOpen] = useState(false);
  const [editingSuspectId, setEditingSuspectId] = useState(null);
  const [sFullName, setSFullName] = useState('');
  const [sAlias, setSAlias] = useState('');
  const [sAge, setSAge] = useState('');
  const [sGender, setSGender] = useState('MALE');
  const [sPhone, setSPhone] = useState('');
  const [sAddress, setSAddress] = useState('');
  const [sOccupation, setSOccupation] = useState('');
  const [sDescription, setSDescription] = useState('');
  const [sStatus, setSStatus] = useState('UNDER_INVESTIGATION');
  const [savingSuspect, setSavingSuspect] = useState(false);

  // Phase 6: Evidence Modals & State
  const [isUploadEvidenceModalOpen, setIsUploadEvidenceModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [evTitle, setEvTitle] = useState('');
  const [evDescription, setEvDescription] = useState('');
  const [evType, setEvType] = useState('DOCUMENT');
  const [evCollectedAt, setEvCollectedAt] = useState('');
  const [evCollectedLocation, setEvCollectedLocation] = useState('');
  const [evCollectedBy, setEvCollectedBy] = useState('');
  const [evNotes, setEvNotes] = useState('');
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadingEvidence, setUploadingEvidence] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [crossCaseWarning, setCrossCaseWarning] = useState(null);

  // Evidence Detail Modal
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedEvidence, setSelectedEvidence] = useState(null);
  const [evidenceAuditLogs, setEvidenceAuditLogs] = useState([]);
  const [statusUpdateVal, setStatusUpdateVal] = useState('');
  const [statusNotes, setStatusNotes] = useState('');
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [copiedHash, setCopiedHash] = useState(false);

  // Edit Evidence Metadata Modal
  const [isEditEvidenceModalOpen, setIsEditEvidenceModalOpen] = useState(false);
  const [editingEvidenceId, setEditingEvidenceId] = useState(null);
  const [editEvTitle, setEditEvTitle] = useState('');
  const [editEvDescription, setEditEvDescription] = useState('');
  const [editEvType, setEditEvType] = useState('DOCUMENT');
  const [editEvCollectedAt, setEditEvCollectedAt] = useState('');
  const [editEvCollectedLocation, setEditEvCollectedLocation] = useState('');
  const [editEvCollectedBy, setEditEvCollectedBy] = useState('');
  const [editEvNotes, setEditEvNotes] = useState('');
  const [savingEvidence, setSavingEvidence] = useState(false);

  // Evidence Type Filter Pill
  const [evidenceFilter, setEvidenceFilter] = useState('ALL');

  // Phase 13: Case Notifications & Dispatch Audit State
  const [caseNotifications, setCaseNotifications] = useState([]);
  const [loadingNotifications, setLoadingNotifications] = useState(false);
  const [isTestAlertModalOpen, setIsTestAlertModalOpen] = useState(false);
  const [testAlertChannel, setTestAlertChannel] = useState('EMAIL');
  const [sendingTestAlert, setSendingTestAlert] = useState(false);
  const [testAlertSuccess, setTestAlertSuccess] = useState('');
  const [testAlertError, setTestAlertError] = useState('');
  const [selectedWaNotif, setSelectedWaNotif] = useState(null);
  const [isWaModalOpen, setIsWaModalOpen] = useState(false);

  // Phase 14: Public News & Search Intelligence State
  const [publicSearchResults, setPublicSearchResults] = useState([]);
  const [publicSearchQuery, setPublicSearchQuery] = useState('');
  const [publicSearchType, setPublicSearchType] = useState('NEWS');
  const [publicSearchDateFrom, setPublicSearchDateFrom] = useState('');
  const [publicSearchDateTo, setPublicSearchDateTo] = useState('');
  const [publicSearchProvider, setPublicSearchProvider] = useState('');
  const [searchingPublic, setSearchingPublic] = useState(false);
  const [publicSearchError, setPublicSearchError] = useState('');
  const [suggestedQueries, setSuggestedQueries] = useState([]);
  const [loadingSuggestedQueries, setLoadingSuggestedQueries] = useState(false);
  const [savedPublicItems, setSavedPublicItems] = useState([]);
  const [loadingSavedPublicItems, setLoadingSavedPublicItems] = useState(false);
  const [savingItemUrl, setSavingItemUrl] = useState('');
  const [isReviewPublicModalOpen, setIsReviewPublicModalOpen] = useState(false);
  const [reviewingPublicItem, setReviewingPublicItem] = useState(null);
  const [reviewVerificationStatus, setReviewVerificationStatus] = useState('UNVERIFIED');
  const [reviewNotes, setReviewNotes] = useState('');
  const [savingPublicReview, setSavingPublicReview] = useState(false);
  const [publicReviewError, setPublicReviewError] = useState('');

  // Phase 15: PDF Report State
  const [generatingPdf, setGeneratingPdf] = useState(false);
  const [pdfError, setPdfError] = useState('');

  // Phase 17: AI Scene Illustrations & Photos State
  const [illustrations, setIllustrations] = useState([]);
  const [loadingIllustrations, setLoadingIllustrations] = useState(false);
  const [generatingIllustration, setGeneratingIllustration] = useState(false);
  const [illustrationPrompt, setIllustrationPrompt] = useState('');
  const [illustrationAspectRatio, setIllustrationAspectRatio] = useState('1:1');
  const [illustrationError, setIllustrationError] = useState('');
  const [illustrationSuccess, setIllustrationSuccess] = useState('');

  // Suspect Photo Upload State
  const [isSuspectPhotoModalOpen, setIsSuspectPhotoModalOpen] = useState(false);
  const [selectedSuspectForPhoto, setSelectedSuspectForPhoto] = useState(null);
  const [suspectPhotoFile, setSuspectPhotoFile] = useState(null);
  const [suspectPhotoPreview, setSuspectPhotoPreview] = useState('');
  const [uploadingSuspectPhoto, setUploadingSuspectPhoto] = useState(false);
  const [suspectPhotoError, setSuspectPhotoError] = useState('');

  // Victim Photo Upload State
  const [isVictimPhotoModalOpen, setIsVictimPhotoModalOpen] = useState(false);
  const [selectedVictimForPhoto, setSelectedVictimForPhoto] = useState(null);
  const [victimPhotoFile, setVictimPhotoFile] = useState(null);
  const [victimPhotoPreview, setVictimPhotoPreview] = useState('');
  const [uploadingVictimPhoto, setUploadingVictimPhoto] = useState(false);
  const [victimPhotoError, setVictimPhotoError] = useState('');

  // Evidence Real Photo Preview
  const [evidencePhotoPreview, setEvidencePhotoPreview] = useState('');

  // Evidence AI Illustration Modal State
  const [isEvidenceAiModalOpen, setIsEvidenceAiModalOpen] = useState(false);
  const [selectedEvidenceForAi, setSelectedEvidenceForAi] = useState(null);
  const [evidenceAiPrompt, setEvidenceAiPrompt] = useState('');
  const [generatingEvidenceAi, setGeneratingEvidenceAi] = useState(false);
  const [evidenceAiError, setEvidenceAiError] = useState('');

  // Replace Real Evidence Modal State
  const [isReplaceModalOpen, setIsReplaceModalOpen] = useState(false);
  const [selectedEvidenceForReplace, setSelectedEvidenceForReplace] = useState(null);
  const [replaceFile, setReplaceFile] = useState(null);
  const [replacePreview, setReplacePreview] = useState('');
  const [replacingFile, setReplacingFile] = useState(false);
  const [replaceError, setReplaceError] = useState('');

  // Lightbox Modal State
  const [activeLightboxImage, setActiveLightboxImage] = useState(null);

  const fetchIllustrations = useCallback(async () => {
    if (!id) return;
    try {
      setLoadingIllustrations(true);
      const data = await getCaseIllustrations(id);
      setIllustrations(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to fetch AI illustrations:', err);
    } finally {
      setLoadingIllustrations(false);
    }
  }, [id]);

  const handleGenerateIllustration = async (e) => {
    e?.preventDefault?.();
    if (!illustrationPrompt || !illustrationPrompt.trim()) {
      setIllustrationError('Please enter a descriptive prompt for AI scene generation.');
      return;
    }
    setGeneratingIllustration(true);
    setIllustrationError('');
    setIllustrationSuccess('');
    try {
      const res = await generateCaseIllustration(id, {
        prompt: illustrationPrompt.trim(),
        aspectRatio: illustrationAspectRatio
      });
      setIllustrationSuccess('AI illustration generated successfully. Notice: Synthetic visual — NOT verified evidence.');
      setIllustrationPrompt('');
      await fetchIllustrations();
      showFlash('AI SCENE ILLUSTRATION GENERATED (NON-EVIDENCE)');
    } catch (err) {
      console.error('AI Illustration generation failed:', err);
      setIllustrationError(err.message || 'Failed to generate AI illustration.');
    } finally {
      setGeneratingIllustration(false);
    }
  };

  const handleDeleteIllustration = async (illustrationId) => {
    if (!window.confirm('Delete this AI-generated scene illustration from case repository?')) return;
    try {
      await deleteCaseIllustration(id, illustrationId);
      await fetchIllustrations();
      showFlash('AI illustration removed');
    } catch (err) {
      alert(err.message || 'Failed to delete illustration');
    }
  };

  const handleOpenEvidenceAiModal = (evidence) => {
    setSelectedEvidenceForAi(evidence);
    setEvidenceAiPrompt(evidence.description ? `${evidence.title} — ${evidence.description}` : `${evidence.title} — crime scene situational reconstruction`);
    setEvidenceAiError('');
    setIsEvidenceAiModalOpen(true);
  };

  const handleGenerateEvidenceIllustration = async (e) => {
    e?.preventDefault?.();
    if (!evidenceAiPrompt || !evidenceAiPrompt.trim()) {
      setEvidenceAiError('Please enter a descriptive prompt for AI scene generation.');
      return;
    }
    if (!selectedEvidenceForAi) return;

    setGeneratingEvidenceAi(true);
    setEvidenceAiError('');
    try {
      await generateEvidenceIllustration(id, selectedEvidenceForAi.id, evidenceAiPrompt.trim());
      setIsEvidenceAiModalOpen(false);
      showFlash('AI SCENE ILLUSTRATION GENERATED (NON-EVIDENCE)');
      const [updatedEvidence, refreshedCase] = await Promise.all([
        getCaseEvidence(id),
        getCaseById(id)
      ]);
      setEvidenceList(updatedEvidence || []);
      setCaseData(refreshedCase);
      await fetchIllustrations();
    } catch (err) {
      console.error('Failed to generate AI illustration for evidence:', err);
      setEvidenceAiError(err.message || 'Failed to generate AI illustration');
    } finally {
      setGeneratingEvidenceAi(false);
    }
  };

  const handleOpenRealUploadModal = (evidence) => {
    setSelectedEvidenceForReplace(evidence);
    setReplaceFile(null);
    setReplacePreview('');
    setReplaceError('');
    setIsReplaceModalOpen(true);
  };

  const handleReplaceFileSelection = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 50 * 1024 * 1024) {
        setReplaceError('Selected file exceeds maximum limit of 50 MB.');
        return;
      }
      setReplaceFile(file);
      setReplaceError('');
      if (file.type.startsWith('image/')) {
        const previewUrl = URL.createObjectURL(file);
        setReplacePreview(previewUrl);
      } else {
        setReplacePreview('');
      }
    }
  };

  const handleReplaceEvidenceSubmit = async (e) => {
    e?.preventDefault?.();
    if (!replaceFile) {
      setReplaceError('Please select a valid forensic evidence file to upload.');
      return;
    }
    if (!selectedEvidenceForReplace) return;

    setReplacingFile(true);
    setReplaceError('');
    try {
      const formData = new FormData();
      formData.append('file', replaceFile);

      await replaceEvidenceFile(id, selectedEvidenceForReplace.id, formData);
      setIsReplaceModalOpen(false);
      showFlash('REAL FORENSIC EVIDENCE STORED (REPLACED SPECIMEN)');

      const [updatedList, refreshedCase] = await Promise.all([
        getCaseEvidence(id),
        getCaseById(id)
      ]);
      setEvidenceList(updatedList || []);
      setCaseData(refreshedCase);
      refreshAnalysisData();
    } catch (err) {
      console.error('Failed to replace evidence with real file:', err);
      setReplaceError(err.message || 'Failed to upload real evidence file');
    } finally {
      setReplacingFile(false);
    }
  };

  const handleOpenSuspectPhotoModal = (suspect) => {
    setSelectedSuspectForPhoto(suspect);
    setSuspectPhotoFile(null);
    setSuspectPhotoPreview(suspect.photoUrl ? `${resolveAssetUrl(suspect.photoUrl)}?t=${Date.now()}` : '');
    setSuspectPhotoError('');
    setIsSuspectPhotoModalOpen(true);
  };

  const handleSuspectPhotoSelection = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        setSuspectPhotoError('Please select a valid image file (JPG, PNG, WEBP).');
        return;
      }
      if (file.size > 25 * 1024 * 1024) {
        setSuspectPhotoError('Image size exceeds maximum limit of 25 MB.');
        return;
      }
      setSuspectPhotoFile(file);
      setSuspectPhotoError('');
      const previewUrl = URL.createObjectURL(file);
      setSuspectPhotoPreview(previewUrl);
    }
  };

  const handleSaveSuspectPhoto = async (e) => {
    e?.preventDefault?.();
    if (!selectedSuspectForPhoto || !suspectPhotoFile) {
      setSuspectPhotoError('Please select an image file to upload.');
      return;
    }
    setUploadingSuspectPhoto(true);
    setSuspectPhotoError('');
    try {
      const formData = new FormData();
      formData.append('file', suspectPhotoFile);
      await uploadSuspectPhoto(id, selectedSuspectForPhoto.id, formData);
      setIsSuspectPhotoModalOpen(false);
      showFlash('SUSPECT PHOTOGRAPH RECORDED');
      const updatedSuspects = await getCaseSuspects(id);
      setSuspects(updatedSuspects || []);
    } catch (err) {
      setSuspectPhotoError(err.message || 'Failed to upload suspect photo');
    } finally {
      setUploadingSuspectPhoto(false);
    }
  };

  const handleOpenVictimPhotoModal = (victim) => {
    setSelectedVictimForPhoto(victim);
    setVictimPhotoFile(null);
    const initialPreview = (victim.imageSource === 'UPLOADED' || victim.hasRealPhoto) && victim.photoUrl
      ? `${resolveAssetUrl(victim.photoUrl)}?t=${Date.now()}`
      : '';
    setVictimPhotoPreview(initialPreview);
    setVictimPhotoError('');
    setIsVictimPhotoModalOpen(true);
  };

  const handleVictimPhotoSelection = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        setVictimPhotoError('Please select a valid image file (JPG, PNG, WEBP).');
        return;
      }
      if (file.size > 25 * 1024 * 1024) {
        setVictimPhotoError('Image size exceeds maximum limit of 25 MB.');
        return;
      }
      setVictimPhotoFile(file);
      setVictimPhotoError('');
      const previewUrl = URL.createObjectURL(file);
      setVictimPhotoPreview(previewUrl);
    }
  };

  const handleSaveVictimPhoto = async (e) => {
    e?.preventDefault?.();
    if (!selectedVictimForPhoto || !victimPhotoFile) {
      setVictimPhotoError('Please select an image file to upload.');
      return;
    }
    setUploadingVictimPhoto(true);
    setVictimPhotoError('');
    try {
      const formData = new FormData();
      formData.append('file', victimPhotoFile);
      await uploadVictimPhoto(id, selectedVictimForPhoto.id, formData);
      setIsVictimPhotoModalOpen(false);
      showFlash('VICTIM PHOTOGRAPH RECORDED');
      const updatedVictims = await getCaseVictims(id);
      setVictims(updatedVictims || []);
    } catch (err) {
      setVictimPhotoError(err.message || 'Failed to upload victim photo');
    } finally {
      setUploadingVictimPhoto(false);
    }
  };

  const handleGeneratePdfReport = async () => {
    if (!id) return;
    try {
      setGeneratingPdf(true);
      setPdfError('');
      const blob = await downloadCaseReportPdf(id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${caseData?.caseNumber || 'case'}-report.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      setSuccessMessage('Case report PDF generated and downloaded successfully!');
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err) {
      console.error('Failed to generate PDF report:', err);
      setPdfError(err.message || 'Failed to generate PDF report');
    } finally {
      setGeneratingPdf(false);
    }
  };

  const fetchSavedPublicItems = useCallback(async () => {
    if (!id) return;
    try {
      setLoadingSavedPublicItems(true);
      const data = await getCasePublicInformation(id);
      const itemsList = Array.isArray(data) ? data : (data?.content || data?.items || data?.references || []);
      setSavedPublicItems(itemsList);
    } catch (err) {
      console.error('Failed to fetch saved public information:', err);
    } finally {
      setLoadingSavedPublicItems(false);
    }
  }, [id]);

  const fetchSuggestedQueries = useCallback(async () => {
    if (!id) return;
    try {
      setLoadingSuggestedQueries(true);
      const data = await getCaseSuggestedQueries(id);
      setSuggestedQueries(data?.suggestedQueries || []);
    } catch (err) {
      console.error('Failed to fetch suggested queries:', err);
    } finally {
      setLoadingSuggestedQueries(false);
    }
  }, [id]);

  const handleExecutePublicSearch = async (overrideQuery = null) => {
    const q = overrideQuery || publicSearchQuery;
    if (!q || q.trim().length < 3) {
      setPublicSearchError('Search query must be at least 3 characters.');
      return;
    }
    if (publicSearchDateFrom && publicSearchDateTo && publicSearchDateFrom > publicSearchDateTo) {
      setPublicSearchError('From-date cannot be later than To-date.');
      return;
    }
    setSearchingPublic(true);
    setPublicSearchError('');
    try {
      const payload = {
        query: q.trim(),
        searchType: publicSearchType,
        dateFrom: publicSearchDateFrom || null,
        dateTo: publicSearchDateTo || null,
      };
      const res = await searchCasePublicInformation(id, payload);
      setPublicSearchResults(res?.results || []);
      setPublicSearchProvider(res?.provider || '');
    } catch (err) {
      console.error('Public search execution error:', err);
      setPublicSearchError(err.message || 'Public information search failed or provider unavailable.');
      setPublicSearchResults([]);
      setPublicSearchProvider('');
    } finally {
      setSearchingPublic(false);
    }
  };

  const handleSavePublicItem = async (item) => {
    if (!item || !item.url) {
      setPublicSearchError('Cannot save item: missing article URL.');
      return;
    }
    setSavingItemUrl(item.url);
    setPublicSearchError('');
    try {
      const payload = {
        title: item.title || 'Untitled Public Information',
        description: item.description || '',
        sourceName: item.sourceName || 'Unknown Source',
        sourceUrl: item.url,
        publishedAt: item.publishedAt || null,
        author: item.author || null,
        imageUrl: item.imageUrl || null,
        originalQuery: item.query || publicSearchQuery || '',
        notes: 'Saved for investigator evaluation.'
      };
      const savedRes = await savePublicInformation(id, payload);
      if (savedRes) {
        setSavedPublicItems((prev) => {
          const list = Array.isArray(prev) ? prev : [];
          const exists = list.some(p => p.id === savedRes.id || p.sourceUrl?.trim()?.toLowerCase() === savedRes.sourceUrl?.trim()?.toLowerCase());
          return exists ? list.map(p => (p.id === savedRes.id || p.sourceUrl === savedRes.sourceUrl ? savedRes : p)) : [savedRes, ...list];
        });
      }
      await fetchSavedPublicItems();
      showFlash('Public information saved successfully');
    } catch (err) {
      console.error('Failed to save public news item:', err);
      const msg = err.message || 'Failed to save article to case.';
      setPublicSearchError(msg);
      showFlash(msg);
    } finally {
      setSavingItemUrl('');
    }
  };

  const handleOpenReviewModal = (item) => {
    setReviewingPublicItem(item);
    setReviewVerificationStatus(item.verificationStatus || 'UNVERIFIED');
    setReviewNotes(item.notes || '');
    setPublicReviewError('');
    setIsReviewPublicModalOpen(true);
  };

  const handleSavePublicReview = async (e) => {
    e?.preventDefault?.();
    if (!reviewingPublicItem) return;

    if ((reviewVerificationStatus === 'CORROBORATED' ||
         reviewVerificationStatus === 'CONTRADICTED' ||
         reviewVerificationStatus === 'NOT_RELEVANT') &&
        (!reviewNotes || !reviewNotes.trim())) {
      setPublicReviewError(`Investigator notes are mandatory when marking public information as ${reviewVerificationStatus}`);
      return;
    }

    setSavingPublicReview(true);
    setPublicReviewError('');
    try {
      await updatePublicInformationItem(id, reviewingPublicItem.id, {
        verificationStatus: reviewVerificationStatus,
        notes: reviewNotes.trim()
      });
      await fetchSavedPublicItems();
      setIsReviewPublicModalOpen(false);
      showFlash(`Public information updated: ${reviewVerificationStatus}`);
    } catch (err) {
      setPublicReviewError(err.message || 'Failed to update verification status');
    } finally {
      setSavingPublicReview(false);
    }
  };

  const handleDeletePublicItem = async (itemId) => {
    if (!window.confirm('Remove this public information record from case audit?')) return;
    try {
      await deletePublicInformationItem(id, itemId);
      await fetchSavedPublicItems();
      showFlash('Public information item removed.');
    } catch (err) {
      showFlash('Failed to delete item: ' + err.message);
    }
  };

  const fetchCaseNotifications = useCallback(async () => {
    if (!id) return;
    try {
      setLoadingNotifications(true);
      const data = await getNotifications(id);
      setCaseNotifications(data || []);
    } catch (err) {
      console.error('Failed to fetch case notifications:', err);
    } finally {
      setLoadingNotifications(false);
    }
  }, [id]);

  const refreshAnalysisData = useCallback(async () => {
    try {
      const [stateRes, gapsRes] = await Promise.all([
        getCaseState(id).catch(() => null),
        getEvidenceGaps(id).catch(() => []),
      ]);
      setCaseState(stateRes);
      setEvidenceGaps(gapsRes || []);
    } catch (err) {
      console.error('Failed to refresh analysis data:', err);
    }
  }, [id]);

  const handleGapUpdatedInCase = useCallback((updatedGap) => {
    if (updatedGap) {
      if (Array.isArray(updatedGap)) {
        setEvidenceGaps(updatedGap);
      } else if (updatedGap.id) {
        setEvidenceGaps(prevGaps => prevGaps.map(g => g.id === updatedGap.id ? { ...g, ...updatedGap } : g));
      }
    }
    refreshAnalysisData();
  }, [refreshAnalysisData]);

  const fetchWitnesses = useCallback(async () => {
    if (!id) return;
    try {
      setLoadingWitnesses(true);
      const data = await getCaseWitnesses(id);
      setWitnesses(data || []);
    } catch (err) {
      console.error('Failed to fetch case witnesses:', err);
    } finally {
      setLoadingWitnesses(false);
    }
  }, [id]);

  const fetchTimeline = useCallback(async () => {
    if (!id) return;
    try {
      setLoadingTimeline(true);
      const data = await getCaseTimeline(id);
      setTimelineEvents(data || []);
    } catch (err) {
      console.error('Failed to fetch case timeline:', err);
    } finally {
      setLoadingTimeline(false);
    }
  }, [id]);

  const fetchReportHistory = useCallback(async () => {
    if (!id) return;
    try {
      setLoadingReportHistory(true);
      const data = await getCaseReportHistory(id);
      setReportHistory(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to fetch report history:', err);
    } finally {
      setLoadingReportHistory(false);
    }
  }, [id]);

  const loadCaseAndChildData = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [caseRes, firRes, victimsRes, suspectsRes, witnessesRes, evidenceRes, timelineRes, stateRes, gapsRes, notifsRes, publicRes, suggestionsRes, officersRes, illustrationsRes] = await Promise.all([
        getCaseById(id),
        getCaseFIR(id).catch(() => null),
        getCaseVictims(id).catch(() => []),
        getCaseSuspects(id).catch(() => []),
        getCaseWitnesses(id).catch(() => []),
        getCaseEvidence(id).catch(() => []),
        getCaseTimeline(id).catch(() => []),
        getCaseState(id).catch(() => null),
        getEvidenceGaps(id).catch(() => []),
        getNotifications(id).catch(() => []),
        getCasePublicInformation(id).catch(() => []),
        getCaseSuggestedQueries(id).catch(() => ({ suggestedQueries: [] })),
        getCaseOfficers(id).catch(() => []),
        getCaseIllustrations(id).catch(() => []),
      ]);

      if (caseRes) {
        setCaseData(caseRes);
        setEditTitle(caseRes.title || '');
        setEditDescription(caseRes.description || '');
        setEditType(caseRes.caseType || 'MURDER');
        setEditPriority(caseRes.priority || 'MEDIUM');
        setEditStatus(caseRes.status || 'OPEN');
      }

      setFirData(firRes);
      setVictims(victimsRes || []);
      setSuspects(suspectsRes || []);
      setWitnesses(witnessesRes || []);
      setEvidenceList(evidenceRes || []);
      setTimelineEvents(timelineRes || []);
      setCaseState(stateRes);
      setEvidenceGaps(gapsRes || []);
      setCaseNotifications(notifsRes || []);
      setSavedPublicItems(Array.isArray(publicRes) ? publicRes : (publicRes?.content || publicRes?.items || publicRes?.references || []));
      setSuggestedQueries(suggestionsRes?.suggestedQueries || []);
      setAssignedOfficers(officersRes || []);
      setIllustrations(Array.isArray(illustrationsRes) ? illustrationsRes : []);
    } catch (err) {
      setError(err.message || 'Case record not found or access restricted to your station.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  const handleSendTestAlert = async (e) => {
    e?.preventDefault?.();
    setSendingTestAlert(true);
    setTestAlertError('');
    setTestAlertSuccess('');
    try {
      const res = await sendTestCaseNotification(id, testAlertChannel);
      setTestAlertSuccess(`Test alert dispatched via ${res.channel}. Status: ${res.status}. ID: ${res.id}`);
      await fetchCaseNotifications();
      showFlash(`Test alert triggered: ${res.status}`);
    } catch (err) {
      setTestAlertError(err.response?.data?.message || err.message || 'Failed to dispatch test alert');
    } finally {
      setSendingTestAlert(false);
    }
  };

  useEffect(() => {
    if (id) {
      loadCaseAndChildData();
    }
  }, [id, loadCaseAndChildData]);

  /* ==========================================================================
     CASE HANDLERS
     ========================================================================== */

  const handleStatusChange = async (newStatus) => {
    try {
      const updated = await updateCaseStatus(id, newStatus);
      setCaseData(updated);
      refreshAnalysisData();
      showFlash('CASE STATUS UPDATED');
    } catch (err) {
      alert(err.message || 'Failed to update status');
    }
  };

  const handlePriorityChange = async (newPriority) => {
    try {
      const updated = await updateCasePriority(id, newPriority);
      setCaseData(updated);
      refreshAnalysisData();
      showFlash('CASE PRIORITY UPDATED');
    } catch (err) {
      alert(err.message || 'Failed to update priority');
    }
  };

  const handleSaveFullCaseEdit = async (e) => {
    e.preventDefault();
    setSavingCase(true);
    try {
      const payload = {
        title: editTitle,
        description: editDescription,
        caseType: editType,
        priority: editPriority,
        status: editStatus,
      };
      const updated = await updateCase(id, payload);
      setCaseData(updated);
      setIsEditCaseModalOpen(false);
      refreshAnalysisData();
      showFlash('CASE DOSSIER MODIFICATIONS SAVED');
    } catch (err) {
      alert(err.message || 'Failed to save case changes');
    } finally {
      setSavingCase(false);
    }
  };

  /* ==========================================================================
     FIR HANDLERS
     ========================================================================== */

  const handleOpenFirModal = (fir = null) => {
    if (fir) {
      setIsEditingFir(true);
      setFirComplainant(fir.complainantName || '');
      setFirContact(fir.complainantContact || '');
      setFirRegDate(fir.registrationDate ? fir.registrationDate.substring(0, 16) : '');
      setFirIncDate(fir.incidentDate ? fir.incidentDate.substring(0, 16) : '');
      setFirLocation(fir.incidentLocation || '');
      setFirOffenceType(fir.offenceType || caseData?.caseType || 'HOMICIDE');
      setFirSections(fir.sectionsOfLaw || '');
      setFirDesc(fir.description || '');
    } else {
      setIsEditingFir(false);
      setFirComplainant('');
      setFirContact('');
      setFirRegDate(new Date().toISOString().substring(0, 16));
      setFirIncDate('');
      setFirLocation('');
      setFirOffenceType(caseData?.caseType || 'MURDER');
      setFirSections('');
      setFirDesc('');
    }
    setIsFirModalOpen(true);
  };

  const handleSaveFir = async (e) => {
    e.preventDefault();
    setSavingFir(true);
    try {
      const payload = {
        complainantName: firComplainant.trim(),
        complainantContact: firContact.trim(),
        registrationDate: firRegDate ? new Date(firRegDate).toISOString() : null,
        incidentDate: firIncDate ? new Date(firIncDate).toISOString() : null,
        incidentLocation: firLocation.trim(),
        offenceType: firOffenceType.trim(),
        sectionsOfLaw: firSections.trim(),
        description: firDesc.trim(),
      };

      let result;
      if (isEditingFir) {
        result = await updateCaseFIR(id, payload);
        showFlash('FIRST INFORMATION REPORT UPDATED');
      } else {
        result = await createCaseFIR(id, payload);
        showFlash('FIRST INFORMATION REPORT REGISTERED');
      }

      setFirData(result);
      setIsFirModalOpen(false);
      const refreshedCase = await getCaseById(id);
      setCaseData(refreshedCase);
      refreshAnalysisData();
    } catch (err) {
      alert(err.message || 'Failed to save FIR record');
    } finally {
      setSavingFir(false);
    }
  };

  /* ==========================================================================
     VICTIM HANDLERS
     ========================================================================== */

  const handleOpenVictimModal = (victim = null) => {
    if (victim) {
      setEditingVictimId(victim.id);
      setVFullName(victim.fullName || '');
      setVAge(victim.age || '');
      setVGender(victim.gender || 'MALE');
      setVDob(victim.dateOfBirth || '');
      setVPhone(victim.phone || '');
      setVAddress(victim.address || '');
      setVOccupation(victim.occupation || '');
      setVRelationship(victim.relationshipToCase || 'Primary Victim');
      setVDescription(victim.description || '');
      setVStatus(victim.status || 'ACTIVE');
    } else {
      setEditingVictimId(null);
      setVFullName('');
      setVAge('');
      setVGender('MALE');
      setVDob('');
      setVPhone('');
      setVAddress('');
      setVOccupation('');
      setVRelationship('Primary Victim');
      setVDescription('');
      setVStatus('ACTIVE');
    }
    setIsVictimModalOpen(true);
  };

  const handleSaveVictim = async (e) => {
    e.preventDefault();
    if (!vFullName.trim()) {
      alert('Victim Full Name is required');
      return;
    }
    setSavingVictim(true);
    try {
      const payload = {
        fullName: vFullName.trim(),
        age: vAge ? parseInt(vAge, 10) : null,
        gender: vGender,
        dateOfBirth: vDob || null,
        phone: vPhone.trim(),
        address: vAddress.trim(),
        occupation: vOccupation.trim(),
        relationshipToCase: vRelationship.trim(),
        description: vDescription.trim(),
        status: vStatus,
      };

      if (editingVictimId) {
        await updateCaseVictim(id, editingVictimId, payload);
        showFlash('VICTIM RECORD UPDATED');
      } else {
        await createCaseVictim(id, payload);
        showFlash('VICTIM RECORD REGISTERED');
      }

      const updatedList = await getCaseVictims(id);
      setVictims(updatedList || []);
      setIsVictimModalOpen(false);

      const refreshedCase = await getCaseById(id);
      setCaseData(refreshedCase);
      refreshAnalysisData();
    } catch (err) {
      alert(err.message || 'Failed to save victim record');
    } finally {
      setSavingVictim(false);
    }
  };

  /* ==========================================================================
     SUSPECT HANDLERS
     ========================================================================== */

  const handleOpenSuspectModal = (suspect = null) => {
    if (suspect) {
      setEditingSuspectId(suspect.id);
      setSFullName(suspect.fullName || '');
      setSAlias(suspect.alias || '');
      setSAge(suspect.age || '');
      setSGender(suspect.gender || 'MALE');
      setSPhone(suspect.phone || '');
      setSAddress(suspect.address || '');
      setSOccupation(suspect.occupation || '');
      setSDescription(suspect.description || '');
      setSStatus(suspect.investigationStatus || 'UNDER_INVESTIGATION');
    } else {
      setEditingSuspectId(null);
      setSFullName('');
      setSAlias('');
      setSAge('');
      setSGender('MALE');
      setSPhone('');
      setSAddress('');
      setSOccupation('');
      setSDescription('');
      setSStatus('UNDER_INVESTIGATION');
    }
    setIsSuspectModalOpen(true);
  };

  const handleSaveSuspect = async (e) => {
    e.preventDefault();
    if (!sFullName.trim()) {
      alert('Suspect Full Name is required');
      return;
    }
    setSavingSuspect(true);
    try {
      const payload = {
        fullName: sFullName.trim(),
        alias: sAlias.trim(),
        age: sAge ? parseInt(sAge, 10) : null,
        gender: sGender,
        phone: sPhone.trim(),
        address: sAddress.trim(),
        occupation: sOccupation.trim(),
        description: sDescription.trim(),
        investigationStatus: sStatus,
      };

      if (editingSuspectId) {
        await updateCaseSuspect(id, editingSuspectId, payload);
        showFlash('SUSPECT DOSSIER UPDATED');
      } else {
        await createCaseSuspect(id, payload);
        showFlash('SUSPECT DOSSIER REGISTERED');
      }

      const updatedList = await getCaseSuspects(id);
      setSuspects(updatedList || []);
      setIsSuspectModalOpen(false);

      const refreshedCase = await getCaseById(id);
      setCaseData(refreshedCase);
      refreshAnalysisData();
    } catch (err) {
      alert(err.message || 'Failed to save suspect record');
    } finally {
      setSavingSuspect(false);
    }
  };

  /* ==========================================================================
     PHASE 6: EVIDENCE HANDLERS
     ========================================================================== */

  const handleOpenUploadModal = () => {
    setSelectedFile(null);
    setEvidencePhotoPreview('');
    setEvTitle('');
    setEvDescription('');
    setEvType('DOCUMENT');
    setEvCollectedAt(new Date().toISOString().substring(0, 16));
    setEvCollectedLocation('');
    setEvCollectedBy(caseData?.createdBy?.fullName || '');
    setEvNotes('');
    setUploadProgress(0);
    setUploadError('');
    setCrossCaseWarning(null);
    setIsUploadEvidenceModalOpen(true);
  };

  const handleFileSelection = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 50 * 1024 * 1024) {
        setUploadError('Selected file exceeds maximum limit of 50 MB.');
        return;
      }
      setSelectedFile(file);
      setUploadError('');
      setCrossCaseWarning(null);

      if (file.type.startsWith('image/')) {
        const previewUrl = URL.createObjectURL(file);
        setEvidencePhotoPreview(previewUrl);
        setEvType('IMAGE');
      } else {
        setEvidencePhotoPreview('');
      }

      if (!evTitle) {
        setEvTitle(file.name.replace(/\.[^/.]+$/, '').replace(/[_-]/g, ' '));
      }
    }
  };

  const handleUploadEvidenceSubmit = async (e, confirmDuplicate = false) => {
    if (e && e.preventDefault) e.preventDefault();
    if (!selectedFile) {
      setUploadError('Please select a valid evidence file to upload.');
      return;
    }
    if (!evTitle.trim()) {
      setUploadError('Evidence title is required.');
      return;
    }

    setUploadingEvidence(true);
    setUploadError('');
    setUploadProgress(10);

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('title', evTitle.trim());
      formData.append('description', evDescription.trim());
      formData.append('evidenceType', evType);
      if (evCollectedAt) formData.append('collectedAt', new Date(evCollectedAt).toISOString());
      if (evCollectedLocation) formData.append('collectedLocation', evCollectedLocation.trim());
      if (evCollectedBy) formData.append('collectedBy', evCollectedBy.trim());
      if (evNotes) formData.append('notes', evNotes.trim());
      if (confirmDuplicate) formData.append('confirmDuplicate', 'true');

      await uploadEvidence(id, formData, (progress) => {
        setUploadProgress(progress);
      });

      showFlash(confirmDuplicate ? 'EVIDENCE SECURELY STORED (CROSS-CASE CONFIRMED)' : 'EVIDENCE FILE SECURELY STORED & CATALOGED');
      setCrossCaseWarning(null);
      setIsUploadEvidenceModalOpen(false);

      // Refresh evidence list and parent case
      const [updatedList, refreshedCase] = await Promise.all([
        getCaseEvidence(id),
        getCaseById(id)
      ]);
      setEvidenceList(updatedList || []);
      setCaseData(refreshedCase);
      refreshAnalysisData();
      try {
        window.dispatchEvent(new Event('crimesphere:notification-updated'));
      } catch (e) {}
    } catch (err) {
      if (err.isCrossCaseDuplicate || (err.message && err.message.includes('This file already exists in another case:'))) {
        let matchedCaseNum = err.caseNumber;
        if (!matchedCaseNum && err.message.includes('This file already exists in another case:')) {
          const match = err.message.match(/This file already exists in another case:\s*([^\.\s]+)/);
          if (match && match[1]) matchedCaseNum = match[1];
        }
        setCrossCaseWarning({
          caseNumber: matchedCaseNum || 'another case',
          message: err.message || `This file already exists in another case: ${matchedCaseNum || 'another case'}. It may be relevant to multiple investigations.`
        });
        setUploadError('');
      } else {
        setUploadError(err.message || 'Evidence upload failed.');
        setCrossCaseWarning(null);
      }
    } finally {
      setUploadingEvidence(false);
    }
  };

  const handleOpenDetailModal = async (evidence) => {
    setSelectedEvidence(evidence);
    setStatusUpdateVal(evidence.status || 'UPLOADED');
    setStatusNotes('');
    setCopiedHash(false);
    setIsDetailModalOpen(true);

    try {
      const [detail, logs] = await Promise.all([
        getEvidenceDetail(id, evidence.id).catch(() => evidence),
        getEvidenceAuditLogs(id, evidence.id).catch(() => [])
      ]);
      setSelectedEvidence(detail);
      setEvidenceAuditLogs(logs);
    } catch (err) {
      console.error('Failed to fetch evidence details / audit:', err);
    }
  };

  const handleDownload = async (evidence) => {
    try {
      await downloadEvidenceFile(id, evidence.id, evidence.originalFileName || `${evidence.evidenceNumber}.bin`);
      showFlash(`DOWNLOADING ${evidence.evidenceNumber}`);
    } catch (err) {
      alert(err.message || 'Failed to download evidence file.');
    }
  };

  const handleUpdateStatusSubmit = async (e) => {
    e.preventDefault();
    if (!selectedEvidence) return;
    setUpdatingStatus(true);
    try {
      const updated = await updateEvidenceStatus(id, selectedEvidence.id, statusUpdateVal, statusNotes);
      setSelectedEvidence(updated);
      showFlash(`EVIDENCE STATUS UPDATED TO ${statusUpdateVal}`);

      const [updatedList, logs, refreshedCase] = await Promise.all([
        getCaseEvidence(id),
        getEvidenceAuditLogs(id, selectedEvidence.id).catch(() => []),
        getCaseById(id)
      ]);
      setEvidenceList(updatedList || []);
      setEvidenceAuditLogs(logs);
      setCaseData(refreshedCase);
      refreshAnalysisData();
    } catch (err) {
      alert(err.message || 'Failed to update evidence status');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const handleOpenEditEvidenceModal = (evidence) => {
    setEditingEvidenceId(evidence.id);
    setEditEvTitle(evidence.title || '');
    setEditEvDescription(evidence.description || '');
    setEditEvType(evidence.evidenceType || 'DOCUMENT');
    setEditEvCollectedAt(evidence.collectedAt ? evidence.collectedAt.substring(0, 16) : '');
    setEditEvCollectedLocation(evidence.collectedLocation || '');
    setEditEvCollectedBy(evidence.collectedBy || '');
    setEditEvNotes(evidence.notes || '');
    setIsEditEvidenceModalOpen(true);
  };

  const handleSaveEditEvidenceSubmit = async (e) => {
    e.preventDefault();
    if (!editEvTitle.trim()) {
      alert('Evidence title is required.');
      return;
    }
    setSavingEvidence(true);
    try {
      const payload = {
        title: editEvTitle.trim(),
        description: editEvDescription.trim(),
        evidenceType: editEvType,
        collectedAt: editEvCollectedAt ? new Date(editEvCollectedAt).toISOString() : null,
        collectedLocation: editEvCollectedLocation.trim(),
        collectedBy: editEvCollectedBy.trim(),
        notes: editEvNotes.trim()
      };

      const updated = await updateEvidence(id, editingEvidenceId, payload);
      showFlash('EVIDENCE METADATA MODIFIED');
      setIsEditEvidenceModalOpen(false);

      if (selectedEvidence && selectedEvidence.id === editingEvidenceId) {
        setSelectedEvidence(updated);
      }

      const [updatedList, refreshedCase] = await Promise.all([
        getCaseEvidence(id),
        getCaseById(id)
      ]);
      setEvidenceList(updatedList || []);
      setCaseData(refreshedCase);
      refreshAnalysisData();
    } catch (err) {
      alert(err.message || 'Failed to save evidence changes.');
    } finally {
      setSavingEvidence(false);
    }
  };

  const handleCopyHash = (hash) => {
    if (!hash) return;
    navigator.clipboard.writeText(hash);
    setCopiedHash(true);
    setTimeout(() => setCopiedHash(false), 2500);
  };

  const formatBytes = (bytes, decimals = 2) => {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  };

  const getEvidenceTypeIcon = (type) => {
    switch (type) {
      case 'DOCUMENT': return <FileText size={16} className="ev-icon doc" />;
      case 'IMAGE': return <ImageIcon size={16} className="ev-icon img" />;
      case 'VIDEO':
      case 'CCTV': return <Video size={16} className="ev-icon video" />;
      case 'AUDIO':
      case 'CALL_RECORD': return <Music size={16} className="ev-icon audio" />;
      case 'FORENSIC_REPORT': return <FileCheck size={16} className="ev-icon report" />;
      case 'DIGITAL_DEVICE': return <HardDrive size={16} className="ev-icon device" />;
      default: return <FileCode size={16} className="ev-icon other" />;
    }
  };

  const getEvidenceStatusBadgeClass = (status) => {
    switch (status) {
      case 'UPLOADED': return 'evbadge-uploaded';
      case 'UNDER_REVIEW': return 'evbadge-review';
      case 'VERIFIED': return 'evbadge-verified';
      case 'REJECTED': return 'evbadge-rejected';
      case 'ARCHIVED': return 'evbadge-archived';
      default: return 'evbadge-uploaded';
    }
  };

  const filteredEvidence = evidenceList.filter((item) => {
    if (evidenceFilter === 'ALL') return true;
    return item.evidenceType === evidenceFilter;
  });

  const getPriorityBadgeClass = (priority) => {
    switch (priority) {
      case 'CRITICAL': return 'badge-priority-critical';
      case 'HIGH': return 'badge-priority-high';
      case 'MEDIUM': return 'badge-priority-medium';
      case 'LOW': return 'badge-priority-low';
      default: return 'badge-priority-medium';
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'OPEN': return 'badge-status-open';
      case 'UNDER_INVESTIGATION': return 'badge-status-investigating';
      case 'PENDING_REVIEW': return 'badge-status-review';
      case 'ON_HOLD': return 'badge-status-hold';
      case 'CLOSED': return 'badge-status-closed';
      default: return 'badge-status-open';
    }
  };

  const getVictimStatusClass = (status) => {
    switch (status) {
      case 'DECEASED': return 'vbadge-deceased';
      case 'ACTIVE': return 'vbadge-active';
      case 'MISSING': return 'vbadge-missing';
      case 'IDENTIFIED': return 'vbadge-identified';
      default: return 'vbadge-active';
    }
  };

  const getSuspectStatusClass = (status) => {
    switch (status) {
      case 'UNDER_INVESTIGATION': return 'sbadge-investigating';
      case 'PERSON_OF_INTEREST': return 'sbadge-poi';
      case 'CLEARED': return 'sbadge-cleared';
      case 'REFERRED': return 'sbadge-referred';
      default: return 'sbadge-investigating';
    }
  };

  if (loading) {
    return (
      <div className="cs-dashboard-layout">
        <TopBar />
        <div className="cs-main-wrapper">
          <Sidebar />
          <main className="cs-content-area forensic-grid">
            <div className="loading-state-box">
              <Clock className="spin-icon" size={24} />
              <span>RETRIEVING CLASSIFIED CASE DOSSIER...</span>
            </div>
          </main>
        </div>
      </div>
    );
  }

  if (error || !caseData) {
    return (
      <div className="cs-dashboard-layout">
        <TopBar />
        <div className="cs-main-wrapper">
          <Sidebar />
          <main className="cs-content-area forensic-grid">
            <div className="error-state-card cs-panel">
              <ShieldAlert size={42} className="err-icon" />
              <div className="err-title">RESTRICTED OR NON-EXISTENT CASE DOSSIER</div>
              <div className="err-desc">{error}</div>
              <button className="btn-back-dir" onClick={() => navigate('/cases')}>
                <ArrowLeft size={16} />
                <span>RETURN TO PRECINCT DIRECTORY</span>
              </button>
            </div>
          </main>
        </div>
      </div>
    );
  }

  return (
    <div className="cs-dashboard-layout">
      <TopBar />
      <div className="cs-main-wrapper">
        <Sidebar />

        <main className="cs-content-area forensic-grid">
          {/* Top Navigation & Action Header */}
          <div className="detail-top-nav">
            <button className="btn-back" onClick={() => navigate('/cases')}>
              <ArrowLeft size={15} />
              <span>BACK TO CASES DIRECTORY</span>
            </button>

            <div className="top-actions">
              <button 
                className="btn-generate-pdf"
                onClick={handleGeneratePdfReport}
                disabled={generatingPdf}
                title="Generate official printable PDF investigation case report"
              >
                <Download size={14} className={generatingPdf ? 'spin-icon' : ''} />
                <span>{generatingPdf ? 'GENERATING REPORT...' : 'GENERATE CASE REPORT'}</span>
              </button>

              <button 
                className="btn-edit-case"
                onClick={() => setIsEditCaseModalOpen(true)}
              >
                <Edit3 size={14} />
                <span>EDIT CASE INFO</span>
              </button>
            </div>
          </div>

          {successMessage && (
            <div className="flash-success-banner">
              <CheckCircle size={16} />
              <span>{successMessage}</span>
            </div>
          )}

          {/* Dedicated Case Workspace Header */}
          <CaseHeader
            caseData={caseData}
            caseState={caseState}
            onEditCase={() => setIsEditCaseModalOpen(true)}
            onStatusChange={handleStatusChange}
            onPriorityChange={handlePriorityChange}
          />

          {/* 10 Case-Specific Tabs Bar */}
          <div className="dossier-tabs-bar">
            <button 
              className={`tab-btn ${activeTab === 'OVERVIEW' ? 'active' : ''}`}
              onClick={() => setActiveTab('OVERVIEW')}
            >
              <FileText size={15} />
              <span>OVERVIEW</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'FIR' ? 'active' : ''}`}
              onClick={() => setActiveTab('FIR')}
            >
              <FileBadge size={15} />
              <span>FIR {firData ? `(${firData.firNumber})` : '(UNREGISTERED)'}</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'EVIDENCE' ? 'active' : ''}`}
              onClick={() => setActiveTab('EVIDENCE')}
            >
              <FolderLock size={15} />
              <span>EVIDENCE ({evidenceList.length})</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'VICTIMS' ? 'active' : ''}`}
              onClick={() => setActiveTab('VICTIMS')}
            >
              <UserX size={15} />
              <span>VICTIMS ({victims.length})</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'SUSPECTS' ? 'active' : ''}`}
              onClick={() => setActiveTab('SUSPECTS')}
            >
              <Users size={15} />
              <span>SUSPECTS ({suspects.length})</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'WITNESSES' ? 'active' : ''}`}
              onClick={() => { setActiveTab('WITNESSES'); fetchWitnesses(); }}
            >
              <Eye size={15} />
              <span>WITNESSES ({witnesses.length})</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'AI_INVESTIGATION' || activeTab === 'AI_SERVICE' ? 'active' : ''}`}
              onClick={() => setActiveTab('AI_INVESTIGATION')}
            >
              <Cpu size={15} />
              <span>AI INVESTIGATION</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'TIMELINE' ? 'active' : ''}`}
              onClick={() => { setActiveTab('TIMELINE'); fetchTimeline(); }}
            >
              <Clock size={15} />
              <span>TIMELINE ({timelineEvents.length})</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'PUBLIC_INTEL' ? 'active' : ''}`}
              onClick={() => { setActiveTab('PUBLIC_INTEL'); fetchSavedPublicItems(); fetchSuggestedQueries(); }}
            >
              <Globe size={15} />
              <span>PUBLIC INTELLIGENCE ({savedPublicItems.length})</span>
            </button>

            <button 
              className={`tab-btn ${activeTab === 'GENERATE_REPORT' ? 'active' : ''}`}
              onClick={() => { setActiveTab('GENERATE_REPORT'); fetchReportHistory(); }}
            >
              <FileCheck size={15} />
              <span>GENERATE REPORT</span>
            </button>

            <button 
              className={`tab-btn tab-btn-ai-illustrations ${activeTab === 'AI_ILLUSTRATIONS' ? 'active' : ''}`}
              onClick={() => { setActiveTab('AI_ILLUSTRATIONS'); fetchIllustrations(); }}
            >
              <Sparkles size={15} className="ai-tab-sparkle" />
              <span>AI ILLUSTRATIONS ({illustrations.length})</span>
            </button>

            <button 
              className="tab-btn"
              onClick={() => navigate(`/location-intelligence/${caseId}`)}
              style={{ borderLeft: '1px solid var(--border-color)', color: '#06B6D4' }}
              title="Open GIS Location Intelligence & Route Mapping"
            >
              <MapPin size={15} style={{ color: '#06B6D4' }} />
              <span>LOCATION INTELLIGENCE</span>
            </button>
          </div>

          {/* TAB 1: CASE OVERVIEW */}
          {activeTab === 'OVERVIEW' && (
            <div className="space-y-6">
              {/* Unique Case Command Center */}
              <CaseCommandCenter
                caseData={caseData}
                caseState={caseState}
                evidenceList={evidenceList}
                victims={victims}
                suspects={suspects}
                witnesses={witnesses}
                firData={firData}
                timelineEvents={timelineEvents}
                evidenceGaps={evidenceGaps}
                aiRecommendations={caseState?.aiRecommendations || []}
                onNavigateTab={(tab) => setActiveTab(tab)}
                onOpenUploadEvidence={() => setIsUploadEvidenceModalOpen(true)}
                onOpenAddWitness={() => setActiveTab('WITNESSES')}
              />

              {/* Unique Investigation Relationship View */}
              <InvestigationRelationshipView
                caseData={caseData}
                firData={firData}
                victims={victims}
                suspects={suspects}
                witnesses={witnesses}
                evidenceList={evidenceList}
              />

              <div className="dossier-grid">
                <div className="grid-left-column">
                  <div className="metadata-card cs-panel">
                    <div className="meta-item">
                      <Building2 size={16} className="meta-icon" />
                      <div>
                        <div className="meta-label">POLICE STATION</div>
                        <div className="meta-val">{caseData.station?.stationName || 'PRECINCT HEADQUARTERS'}</div>
                      </div>
                    </div>

                    <div className="meta-item">
                      <UserCheck size={16} className="meta-icon" />
                      <div>
                        <div className="meta-label">PRIMARY INVESTIGATOR</div>
                        <div className="meta-val">
                          {caseData.createdBy?.fullName || 'STATION OFFICER'} ({caseData.createdBy?.policeId || 'TN-POL'})
                        </div>
                      </div>
                    </div>

                    <div className="meta-item">
                      <Tag size={16} className="meta-icon" />
                      <div>
                        <div className="meta-label">CLASSIFICATION</div>
                        <div className="meta-val">{(caseData.caseType || 'OTHER').replace(/_/g, ' ')}</div>
                      </div>
                    </div>

                    <div className="meta-item">
                      <Calendar size={16} className="meta-icon" />
                      <div>
                        <div className="meta-label">DATE REGISTERED</div>
                        <div className="meta-val">
                          {caseData.createdAt ? new Date(caseData.createdAt).toLocaleString() : 'System Default'}
                        </div>
                      </div>
                    </div>
                  </div>

                  <Panel 
                    title="CASE NARRATIVE & INITIAL OBSERVATIONS" 
                    subtitle="Official Investigation Briefing"
                    icon={FileText}
                  >
                    <div className="narrative-content">
                      {caseData.description ? (
                        <p className="narrative-text">{caseData.description}</p>
                      ) : (
                        <p className="narrative-placeholder">No narrative recorded. Click Edit Case Info to add initial incident overview.</p>
                      )}
                    </div>
                  </Panel>
                </div>

                <div className="grid-right-column">
                  <div className="quick-summary-panel cs-panel">
                    <div className="summary-title">INVESTIGATION FILE STATUS</div>
                    <div className="summary-items">
                      <div className="summary-row">
                        <span>FIR Filing:</span>
                        <strong className={firData ? 'text-green' : 'text-amber'}>
                          {firData ? firData.firNumber : 'NOT FILED'}
                        </strong>
                      </div>
                      <div className="summary-row">
                        <span>Identified Victims:</span>
                        <strong>{victims.length} Recorded</strong>
                      </div>
                      <div className="summary-row">
                        <span>Tracked Suspects:</span>
                        <strong>{suspects.length} Persons of Interest</strong>
                      </div>
                      <div className="summary-row">
                        <span>Secured Evidence:</span>
                        <strong className="text-red">{evidenceList.length} Items Cataloged</strong>
                      </div>
                      <div className="summary-row">
                        <span>Last Activity:</span>
                        <span className="mono-date">
                          {caseData.updatedAt ? new Date(caseData.updatedAt).toLocaleString() : 'Recent'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* INVESTIGATION TEAM SECTION */}
              <div className="investigation-team-wrapper">
                <Panel 
                  title="INVESTIGATION TEAM" 
                  subtitle="Assigned Law Enforcement Personnel & Chain of Command"
                  icon={Users}
                >
                  {loading ? (
                    <div className="team-loading-state">
                      <RefreshCw className="animate-spin" size={20} />
                      <span>Loading investigation team officers...</span>
                    </div>
                  ) : !assignedOfficers || assignedOfficers.length === 0 ? (
                    <div className="team-empty-state">
                      <UserX size={36} className="empty-icon" />
                      <p className="empty-title">No Officers Assigned</p>
                      <p className="empty-desc">There are currently no officers assigned to this investigation file.</p>
                    </div>
                  ) : (
                    <div className="investigation-team-grid">
                      {assignedOfficers.map((officer, index) => {
                        const isPrimary = (
                          officer.policeId === 'TN-POL-1001' ||
                          (caseData?.createdBy?.policeId && officer.policeId === caseData.createdBy.policeId) ||
                          officer.assignedBy === 'Creator'
                        );
                        return (
                          <div 
                            key={officer.id || officer.officerId || officer.policeId || index}
                            className={`officer-card ${isPrimary ? 'primary-officer-card' : ''}`}
                          >
                            <div className="officer-header">
                              <div className="officer-avatar-box">
                                <UserCheck size={20} />
                              </div>
                              <div className="officer-identity">
                                <h3 className="officer-name">{officer.fullName || 'Officer'}</h3>
                                <p className="officer-id">ID: {officer.policeId || 'N/A'}</p>
                              </div>
                            </div>

                            <div className="officer-badges">
                              {isPrimary ? (
                                <span className="assignment-badge primary-badge">
                                  <ShieldAlert size={12} />
                                  PRIMARY INVESTIGATOR
                                </span>
                              ) : (
                                <span className="assignment-badge collaborating-badge">
                                  <Users size={12} />
                                  COLLABORATING OFFICER
                                </span>
                              )}

                              {officer.active !== false ? (
                                <span className="status-badge active-badge">
                                  <span className="pulse-green-dot"></span>
                                  ACTIVE
                                </span>
                              ) : (
                                <span className="status-badge inactive-badge">
                                  INACTIVE
                                </span>
                              )}
                            </div>

                            <div className="officer-meta-list">
                              <div className="officer-meta">
                                <span className="meta-label">Role / Rank:</span>
                                <span className="meta-value">{officer.rankDesignation || officer.role || 'OFFICER'}</span>
                              </div>

                              <div className="officer-meta">
                                <span className="meta-label">Phone:</span>
                                <span className="meta-value">{officer.phone || 'N/A'}</span>
                              </div>

                              <div className="officer-meta">
                                <span className="meta-label">Station:</span>
                                <span className="meta-value">{officer.stationName && officer.stationName !== 'N/A' ? officer.stationName : (caseData?.station?.stationName || 'N/A')}</span>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </Panel>
              </div>

              {/* Evidence Gap Dashboard under Overview */}
              <div className="mt-6">
                <EvidenceGapDashboard 
                  caseId={id} 
                  gaps={evidenceGaps} 
                  loading={loading && (!evidenceGaps || evidenceGaps.length === 0)}
                  onGapsUpdated={handleGapUpdatedInCase} 
                />
              </div>
            </div>
          )}

          {/* TAB: EVIDENCE GAPS */}
          {activeTab === 'GAPS' && (
            <div className="space-y-6">
              <EvidenceGapDashboard 
                caseId={id} 
                gaps={evidenceGaps} 
                loading={loading && (!evidenceGaps || evidenceGaps.length === 0)}
                onGapsUpdated={handleGapUpdatedInCase} 
              />
            </div>
          )}

          {/* TAB 2: FIR (FIRST INFORMATION REPORT) */}
          {activeTab === 'FIR' && (
            <div className="fir-tab-container">
              {firData ? (
                <div className="fir-dossier-card cs-panel cs-panel-accent">
                  <div className="fir-top-bar">
                    <div className="fir-number-badge">
                      <FileBadge size={20} className="fir-badge-icon" />
                      <div>
                        <div className="fir-label">FIRST INFORMATION REPORT</div>
                        <div className="fir-num">{firData.firNumber}</div>
                      </div>
                    </div>

                    <button className="btn-edit-fir" onClick={() => handleOpenFirModal(firData)}>
                      <Edit3 size={15} />
                      <span>EDIT FIR</span>
                    </button>
                  </div>

                  <div className="fir-details-grid">
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">POLICE STATION</span>
                      <span className="fir-meta-val">{firData.policeStation}</span>
                    </div>
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">REGISTRATION DATE</span>
                      <span className="fir-meta-val">
                        {firData.registrationDate ? new Date(firData.registrationDate).toLocaleString() : 'Registered'}
                      </span>
                    </div>
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">COMPLAINANT NAME</span>
                      <span className="fir-meta-val">{firData.complainantName || 'State Prosecution'}</span>
                    </div>
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">COMPLAINANT CONTACT</span>
                      <span className="fir-meta-val">{firData.complainantContact || 'N/A'}</span>
                    </div>
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">INCIDENT DATE & TIME</span>
                      <span className="fir-meta-val">
                        {firData.incidentDate ? new Date(firData.incidentDate).toLocaleString() : 'Under Investigation'}
                      </span>
                    </div>
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">INCIDENT LOCATION</span>
                      <span className="fir-meta-val">{firData.incidentLocation || 'Scene of Crime'}</span>
                    </div>
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">OFFENCE CLASSIFICATION</span>
                      <span className="fir-meta-val text-red">{firData.offenceType}</span>
                    </div>
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">SECTIONS OF LAW</span>
                      <span className="fir-meta-val mono-sections">{firData.sectionsOfLaw || 'Sections pending assignment'}</span>
                    </div>
                  </div>

                  <div className="fir-narrative-section">
                    <div className="fir-narrative-title">OFFICIAL FIRST INFORMATION NARRATIVE</div>
                    <div className="fir-narrative-text">
                      {firData.description || 'No detailed FIR narrative provided.'}
                    </div>
                  </div>

                  <div className="fir-footer-meta">
                    <span>Filed By Officer: {firData.createdBy?.fullName || 'Station Officer'} ({firData.createdBy?.policeId || 'TN-POL'})</span>
                    <span>System Record ID: {firData.id}</span>
                  </div>
                </div>
              ) : (
                <div className="fir-empty-card cs-panel">
                  <FilePlus2 size={44} className="empty-fir-icon" />
                  <div className="empty-fir-title">NO FIR REGISTERED FOR THIS CASE</div>
                  <div className="empty-fir-desc">
                    An official First Information Report has not been formalized for investigation record {caseData.caseNumber}.
                  </div>
                  <button className="btn-register-fir" onClick={() => handleOpenFirModal(null)}>
                    <PlusCircle size={16} />
                    <span>REGISTER OFFICIAL FIR</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* TAB 3: VICTIMS */}
          {activeTab === 'VICTIMS' && (
            <div className="sub-entity-tab-container">
              <div className="tab-actions-header">
                <div>
                  <h2 className="section-title">CASE VICTIM REGISTRY</h2>
                  <p className="section-subtitle">Documented victims, condition descriptors, and statement profiles.</p>
                </div>

                <button className="btn-add-entity" onClick={() => handleOpenVictimModal(null)}>
                  <PlusCircle size={16} />
                  <span>ADD VICTIM</span>
                </button>
              </div>

              {victims.length === 0 ? (
                <div className="entity-empty-card cs-panel">
                  <UserX size={38} className="empty-mod-icon" />
                  <div className="empty-title">NO VICTIM RECORDS REGISTERED</div>
                  <div className="empty-desc">No victims have been formally added to this investigation file yet.</div>
                  <button className="btn-add-entity" onClick={() => handleOpenVictimModal(null)}>
                    <PlusCircle size={15} />
                    <span>REGISTER FIRST VICTIM</span>
                  </button>
                </div>
              ) : (
                <div className="entity-cards-grid">
                  {victims.map((v) => {
                    const isUploaded = v.imageSource === 'UPLOADED' || v.hasRealPhoto;
                    const portraitUrl = isUploaded && v.photoUrl 
                      ? resolveAssetUrl(v.photoUrl) 
                      : getVictimDefaultImageUrl(id, v.id);

                    return (
                      <div key={v.id} className="entity-card cs-panel">
                        <div className="entity-card-header">
                          <div>
                            <div className="entity-name">{v.fullName}</div>
                            <div className="entity-sub">{v.relationshipToCase || 'Victim'}</div>
                          </div>
                          <span className={`entity-status-badge ${getVictimStatusClass(v.status)}`}>
                            {v.status || 'ACTIVE'}
                          </span>
                        </div>

                        {/* Victim Portrait Container */}
                        <div 
                          className={`entity-portrait-container ${isUploaded ? 'uploaded-photo-container' : 'synthetic-portrait-container'}`}
                          onClick={() => setActiveLightboxImage({
                            url: portraitUrl,
                            title: isUploaded ? `CASE PHOTO — ${v.fullName}` : `SYNTHETIC DOSSIER PORTRAIT — ${v.fullName}`,
                            badge: isUploaded ? 'CASE PHOTO' : 'SYNTHETIC DEMO IMAGE',
                            badgeType: isUploaded ? 'photo' : 'synthetic',
                            subtitle: isUploaded 
                              ? `Victim Profile: ${v.fullName} (${v.relationshipToCase || 'Victim'}) • Law enforcement photographic record without automated facial recognition inference.`
                              : `Fictional simulation visual for ${v.fullName}. NOT a real person identification.`
                          })}
                          title="Click to view full image"
                        >
                          <img 
                            src={portraitUrl} 
                            alt={v.fullName} 
                            className="entity-card-portrait-img"
                            onError={(e) => {
                              if (!e.target.dataset.triedFallback) {
                                e.target.dataset.triedFallback = 'true';
                                e.target.src = getVictimDefaultImageUrl(id, v.id);
                              }
                            }}
                          />
                          {isUploaded ? (
                            <div className="entity-photo-badge case-photo-badge">
                              <Camera size={11} />
                              <span>CASE PHOTO</span>
                            </div>
                          ) : (
                            <div className="synthetic-demo-label-overlay">
                              <div className="synthetic-label-title">SYNTHETIC DEMO IMAGE</div>
                              <div className="synthetic-label-sub">NOT A REAL PERSON IDENTIFICATION</div>
                            </div>
                          )}
                        </div>

                        {/* Upload / Replace Case Photo Button */}
                        <div className="entity-card-quick-upload">
                          <button 
                            type="button" 
                            className="btn-upload-case-photo-bar"
                            onClick={() => handleOpenVictimPhotoModal(v)}
                          >
                            <Camera size={13} />
                            <span>{isUploaded ? 'REPLACE CASE PHOTO' : 'UPLOAD CASE PHOTO'}</span>
                          </button>
                        </div>

                        <div className="entity-body">
                          <div className="entity-field">
                            <span className="field-lbl">AGE & GENDER:</span>
                            <span className="field-val">{v.age ? `${v.age} YRS` : 'N/A'} • {v.gender || 'UNSPECIFIED'}</span>
                          </div>
                          {v.phone && (
                            <div className="entity-field">
                              <span className="field-lbl">CONTACT:</span>
                              <span className="field-val">{v.phone}</span>
                            </div>
                          )}
                          {v.occupation && (
                            <div className="entity-field">
                              <span className="field-lbl">OCCUPATION:</span>
                              <span className="field-val">{v.occupation}</span>
                            </div>
                          )}
                          {v.address && (
                            <div className="entity-field">
                              <span className="field-lbl">ADDRESS:</span>
                              <span className="field-val">{v.address}</span>
                            </div>
                          )}
                          {v.description && (
                            <div className="entity-desc-box">
                              <span className="field-lbl">STATEMENT / INJURY BRIEF:</span>
                              <p className="field-desc">{v.description}</p>
                            </div>
                          )}
                        </div>

                        <div className="entity-card-footer">
                          <span className="reg-time">Logged: {v.createdAt ? new Date(v.createdAt).toLocaleDateString() : 'Active'}</span>
                          <div className="suspect-card-actions">
                            <button 
                              className="btn-edit-subtle"
                              onClick={() => handleOpenCoordinationModal(v)}
                              title="Send structured jurisdiction transfer or inquiry to another police station"
                              style={{
                                backgroundColor: 'rgba(59, 130, 246, 0.12)',
                                border: '1px solid rgba(59, 130, 246, 0.4)',
                                color: '#60a5fa'
                              }}
                            >
                              <Building2 size={13} />
                              <span>SEND TO OTHER POLICE STATION</span>
                            </button>
                            <button 
                              className="btn-edit-subtle btn-photo-subtle" 
                              onClick={() => handleOpenVictimPhotoModal(v)}
                              title="Upload or replace victim photograph"
                            >
                              <Camera size={13} />
                              <span>PHOTO</span>
                            </button>
                            <button className="btn-edit-subtle" onClick={() => handleOpenVictimModal(v)}>
                              <Edit3 size={13} />
                              <span>EDIT</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* TAB 4: SUSPECTS */}
          {activeTab === 'SUSPECTS' && (
            <div className="sub-entity-tab-container">
              <div className="tab-actions-header">
                <div>
                  <h2 className="section-title">SUSPECT DOSSIER DIRECTORY</h2>
                  <p className="section-subtitle">Persons of interest and alibi verification matrices.</p>
                </div>

                <button className="btn-add-entity" onClick={() => handleOpenSuspectModal(null)}>
                  <PlusCircle size={16} />
                  <span>ADD SUSPECT</span>
                </button>
              </div>

              {/* Mandatory Responsible System Legal Disclaimer */}
              <div className="legal-disclaimer-card">
                <AlertCircle size={16} className="legal-ic" />
                <span>
                  <strong>CRIMESPHERE AI LEGAL NOTICE:</strong> Being listed as a suspect or person of interest does not imply guilt. This system is an investigation decision-support platform for law enforcement intelligence.
                </span>
              </div>

              {suspects.length === 0 ? (
                <div className="entity-empty-card cs-panel">
                  <Users size={38} className="empty-mod-icon" />
                  <div className="empty-title">NO SUSPECTS RECORDED</div>
                  <div className="empty-desc">No suspects or persons of interest are currently cataloged for this case.</div>
                  <button className="btn-add-entity" onClick={() => handleOpenSuspectModal(null)}>
                    <PlusCircle size={15} />
                    <span>ADD FIRST SUSPECT</span>
                  </button>
                </div>
              ) : (
                <div className="entity-cards-grid">
                  {suspects.map((s) => {
                    const isUploaded = s.imageSource === 'UPLOADED' || s.hasRealPhoto;
                    const portraitUrl = isUploaded && s.photoUrl 
                      ? resolveAssetUrl(s.photoUrl) 
                      : getSuspectDefaultImageUrl(id, s.id);

                    return (
                      <div key={s.id} className="entity-card cs-panel cs-panel-accent">
                        <div className="entity-card-header">
                          <div>
                            <div className="entity-name">{s.fullName}</div>
                            <div className="entity-sub">{s.alias ? `ALIAS: "${s.alias}"` : 'NO RECORDED ALIAS'}</div>
                          </div>
                          <span className={`entity-status-badge ${getSuspectStatusClass(s.investigationStatus)}`}>
                            {(s.investigationStatus || 'UNDER_INVESTIGATION').replace(/_/g, ' ')}
                          </span>
                        </div>

                        {/* Suspect Portrait Container */}
                        <div 
                          className={`entity-portrait-container ${isUploaded ? 'uploaded-photo-container' : 'synthetic-portrait-container'}`}
                          onClick={() => setActiveLightboxImage({
                            url: portraitUrl,
                            title: isUploaded ? `CASE PHOTO — ${s.fullName}` : `SYNTHETIC DOSSIER PORTRAIT — ${s.fullName}`,
                            badge: isUploaded ? 'CASE PHOTO' : 'SYNTHETIC DEMO IMAGE',
                            badgeType: isUploaded ? 'photo' : 'synthetic',
                            subtitle: isUploaded 
                              ? `Dossier Subject: ${s.fullName} (${s.alias ? `"${s.alias}"` : 'No Alias'}) • Law enforcement photographic record without automated facial recognition inference.`
                              : `Fictional simulation visual for ${s.fullName} (${s.alias ? `"${s.alias}"` : 'No Alias'}). NOT a real person identification.`
                          })}
                          title="Click to view full photograph"
                        >
                          <img 
                            src={portraitUrl} 
                            alt={s.fullName} 
                            className="entity-card-portrait-img"
                            onError={(e) => {
                              if (!e.target.dataset.triedFallback) {
                                e.target.dataset.triedFallback = 'true';
                                e.target.src = getSuspectDefaultImageUrl(id, s.id);
                              }
                            }}
                          />
                          {isUploaded ? (
                            <div className="entity-photo-badge case-photo-badge">
                              <Camera size={11} />
                              <span>CASE PHOTO</span>
                            </div>
                          ) : (
                            <div className="synthetic-demo-label-overlay">
                              <div className="synthetic-label-title">SYNTHETIC DEMO IMAGE</div>
                              <div className="synthetic-label-sub">NOT A REAL PERSON IDENTIFICATION</div>
                            </div>
                          )}
                        </div>

                        {/* Upload / Replace Case Photo Button */}
                        <div className="entity-card-quick-upload">
                          <button 
                            type="button" 
                            className="btn-upload-case-photo-bar"
                            onClick={() => handleOpenSuspectPhotoModal(s)}
                          >
                            <Camera size={13} />
                            <span>{isUploaded ? 'REPLACE CASE PHOTO' : 'UPLOAD CASE PHOTO'}</span>
                          </button>
                        </div>

                        <div className="entity-body">
                          <div className="entity-field">
                            <span className="field-lbl">AGE & GENDER:</span>
                            <span className="field-val">{s.age ? `${s.age} YRS` : 'N/A'} • {s.gender || 'UNSPECIFIED'}</span>
                          </div>
                          {s.phone && (
                            <div className="entity-field">
                              <span className="field-lbl">CONTACT:</span>
                              <span className="field-val">{s.phone}</span>
                            </div>
                          )}
                          {s.occupation && (
                            <div className="entity-field">
                              <span className="field-lbl">OCCUPATION:</span>
                              <span className="field-val">{s.occupation}</span>
                            </div>
                          )}
                          {s.address && (
                            <div className="entity-field">
                              <span className="field-lbl">RESIDENCE:</span>
                              <span className="field-val">{s.address}</span>
                            </div>
                          )}
                          {s.description && (
                            <div className="entity-desc-box">
                              <span className="field-lbl">INVESTIGATION SUMMARY & LEADS:</span>
                              <p className="field-desc">{s.description}</p>
                            </div>
                          )}
                        </div>

                        <div className="entity-card-footer">
                          <span className="reg-time">Logged: {s.createdAt ? new Date(s.createdAt).toLocaleDateString() : 'Active'}</span>
                          <div className="suspect-card-actions">
                            <button 
                              className="btn-edit-subtle btn-photo-subtle" 
                              onClick={() => handleOpenSuspectPhotoModal(s)}
                              title="Upload or replace suspect photograph"
                            >
                              <Camera size={13} />
                              <span>PHOTO</span>
                            </button>
                            <button className="btn-edit-subtle" onClick={() => handleOpenSuspectModal(s)}>
                              <Edit3 size={13} />
                              <span>EDIT</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* TAB 5: PHASE 6 EVIDENCE VAULT */}
          {activeTab === 'EVIDENCE' && (
            <div className="sub-entity-tab-container">
              <div className="tab-actions-header">
                <div>
                  <h2 className="section-title">CASE EVIDENCE VAULT</h2>
                  <p className="section-subtitle">Forensic objects, digital records, SHA-256 chain of custody, and verified artifacts.</p>
                </div>

                <button className="btn-add-entity" onClick={handleOpenUploadModal}>
                  <UploadCloud size={16} />
                  <span>UPLOAD EVIDENCE</span>
                </button>
              </div>

              {/* Responsible Investigation Notice */}
              <div className="legal-disclaimer-card">
                <AlertCircle size={16} className="legal-ic" />
                <span>
                  <strong>CRIMESPHERE AI FORENSIC NOTICE:</strong> Evidence records support investigation and verification. They do not by themselves establish guilt or innocence. Evidence requires investigator verification.
                </span>
              </div>

              {/* Evidence Type Filter Bar */}
              <div className="evidence-filter-bar">
                {['ALL', 'DOCUMENT', 'IMAGE', 'CCTV', 'VIDEO', 'AUDIO', 'FORENSIC_REPORT', 'CALL_RECORD', 'DIGITAL_DEVICE', 'OTHER'].map((type) => (
                  <button
                    key={type}
                    className={`filter-pill ${evidenceFilter === type ? 'active' : ''}`}
                    onClick={() => setEvidenceFilter(type)}
                  >
                    {type.replace(/_/g, ' ')}
                  </button>
                ))}
              </div>

              {filteredEvidence.length === 0 ? (
                <div className="entity-empty-card cs-panel">
                  <HardDrive size={38} className="empty-mod-icon" />
                  <div className="empty-title">
                    {evidenceFilter === 'ALL' ? 'NO EVIDENCE CATALOGED' : `NO ${evidenceFilter.replace(/_/g, ' ')} EVIDENCE FOUND`}
                  </div>
                  <div className="empty-desc">
                    {evidenceFilter === 'ALL'
                      ? 'Upload documentary, media, CCTV, or forensic artifacts to secure chain-of-custody for this case.'
                      : `No evidence items currently match the filter "${evidenceFilter}".`}
                  </div>
                  <button className="btn-add-entity" onClick={handleOpenUploadModal}>
                    <UploadCloud size={15} />
                    <span>UPLOAD FIRST EVIDENCE</span>
                  </button>
                </div>
              ) : (
                <div className="entity-cards-grid">
                  {filteredEvidence.map((ev) => {
                    const isAiIllustration = Boolean(
                      ev.isAiGenerated || 
                      ev.category === 'AI_ILLUSTRATION' || 
                      ev.status === 'AI_ILLUSTRATION' ||
                      ev.originalFileName?.startsWith('ai-illustration-')
                    );
                    
                    const isRealUploadedImage = !isAiIllustration && Boolean(
                      (ev.mimeType?.startsWith('image/') || ev.evidenceType === 'IMAGE' || (ev.originalFileName && ev.originalFileName.match(/\.(jpg|jpeg|png|webp)$/i))) &&
                      ev.storagePath && ev.fileSize > 0
                    );

                    const defaultSvgMarkup = renderDefaultEvidenceSvg(
                      ev.evidenceType,
                      ev.title,
                      ev.description,
                      ev.evidenceNumber
                    );

                    return (
                      <div key={ev.id} className="entity-card cs-panel cs-panel-accent">
                        <div className="entity-card-header">
                          <div className="ev-header-info">
                            <div className="ev-type-row">
                              {getEvidenceTypeIcon(ev.evidenceType)}
                              <span className="ev-type-text">{ev.evidenceType.replace(/_/g, ' ')}</span>
                            </div>
                            <div className="entity-name">{ev.title}</div>
                            <div className="entity-sub mono-id">{ev.evidenceNumber}</div>
                          </div>
                          <span className={`entity-status-badge ${getEvidenceStatusBadgeClass(ev.status)}`}>
                            {(ev.status || 'UPLOADED').replace(/_/g, ' ')}
                          </span>
                        </div>

                        {/* EVIDENCE CARD IMAGE SECTION */}
                        <div className="evidence-card-image-section">
                          {isRealUploadedImage ? (
                            /* 1. REAL UPLOADED IMAGE STATE */
                            <div 
                              className="evidence-photo-card-wrap real-upload-wrap"
                              onClick={() => setActiveLightboxImage({
                                url: ev.viewUrl ? resolveAssetUrl(ev.viewUrl) : getEvidenceViewUrl(id, ev.id),
                                title: `EVIDENCE RECORD — ${ev.title}`,
                                badge: 'UPLOADED EVIDENCE',
                                badgeType: 'evidence',
                                subtitle: `File: ${ev.originalFileName || 'evidence.jpg'} • Size: ${formatBytes(ev.fileSize)} • SHA-256: ${ev.fileHash || 'Verified Chain-of-Custody'}`
                              })}
                              title="Click to inspect uploaded evidence image"
                            >
                              <div className="badge-uploaded-evidence-banner">
                                <FolderLock size={11} />
                                <span>UPLOADED EVIDENCE</span>
                              </div>
                              <div className="evidence-img-container-inner">
                                <img 
                                  src={ev.viewUrl ? resolveAssetUrl(ev.viewUrl) : getEvidenceViewUrl(id, ev.id)} 
                                  alt={ev.title} 
                                  className="evidence-card-img"
                                  onError={(e) => { e.target.style.display = 'none'; }}
                                />
                              </div>
                            </div>
                          ) : (
                            /* 2. DEFAULT AI ILLUSTRATION SPECIMEN (BASED ON EVIDENCE TYPE / REPORT) */
                            <div 
                              className="evidence-photo-card-wrap ai-generated-wrap"
                              onClick={() => setActiveLightboxImage({
                                url: (ev.isAiGenerated && ev.viewUrl) ? resolveAssetUrl(ev.viewUrl) : null,
                                svgContent: defaultSvgMarkup,
                                title: `DEFAULT FORENSIC SPECIMEN — ${ev.title}`,
                                badge: 'AI-GENERATED ILLUSTRATION — NOT REAL EVIDENCE',
                                badgeType: 'ai',
                                subtitle: `Synthetic Visual Specimen (NOT physical evidence) • Case #${caseData?.caseNumber || 'N/A'}`
                              })}
                              title="Click to view default illustration"
                            >
                              <div className="badge-ai-evidence-banner">
                                <div className="badge-ai-line1">
                                  <ShieldAlert size={10} />
                                  <span>AI-GENERATED ILLUSTRATION</span>
                                </div>
                                <div className="badge-ai-line2">NOT REAL EVIDENCE</div>
                              </div>
                              <div className="evidence-img-container-inner">
                                {ev.viewUrl && ev.isAiGenerated ? (
                                  <img 
                                    src={resolveAssetUrl(ev.viewUrl)} 
                                    alt={ev.title} 
                                    className="evidence-card-img ai-img"
                                    onError={(e) => {
                                      e.target.style.display = 'none';
                                      if (e.target.nextSibling) {
                                        e.target.nextSibling.style.display = 'block';
                                      }
                                    }}
                                  />
                                ) : null}
                                <div 
                                  className="evidence-svg-fallback-wrap"
                                  style={{ display: (ev.viewUrl && ev.isAiGenerated) ? 'none' : 'block', width: '100%', height: '100%' }}
                                  dangerouslySetInnerHTML={{ __html: defaultSvgMarkup }}
                                />
                              </div>
                            </div>
                          )}

                          {/* 3. UPLOAD BUTTON ALWAYS AVAILABLE BELOW / ON CARD */}
                          <div className="evidence-card-upload-action-row">
                            <button 
                              type="button" 
                              className="btn-upload-evidence-image"
                              onClick={() => handleOpenRealUploadModal(ev)}
                              title={isRealUploadedImage ? "Replace current image with new evidence file" : "Upload authentic evidence photo/file to replace default illustration"}
                            >
                              <UploadCloud size={14} />
                              <span>UPLOAD EVIDENCE IMAGE</span>
                            </button>
                          </div>
                        </div>

                        <div className="entity-body">
                          <div className="entity-field">
                            <span className="field-lbl">FILE ASSET:</span>
                            <span className="field-val truncate-file" title={ev.originalFileName}>
                              {ev.originalFileName || 'file_attachment'}
                            </span>
                          </div>

                          <div className="entity-field">
                            <span className="field-lbl">SIZE & MIME:</span>
                            <span className="field-val">
                              {formatBytes(ev.fileSize)} • {ev.mimeType || 'unknown'}
                            </span>
                          </div>

                          {ev.collectedLocation && (
                            <div className="entity-field">
                              <span className="field-lbl">COLLECTED AT:</span>
                              <span className="field-val">{ev.collectedLocation}</span>
                            </div>
                          )}

                          {ev.fileHash && (
                            <div className="entity-field">
                              <span className="field-lbl">SHA-256 HASH:</span>
                              <span className="field-val mono-hash" title={ev.fileHash}>
                                {ev.fileHash.substring(0, 16)}...
                              </span>
                            </div>
                          )}

                          {ev.description && (
                            <div className="entity-desc-box">
                              <span className="field-lbl">EVIDENCE BRIEF:</span>
                              <p className="field-desc">{ev.description}</p>
                            </div>
                          )}
                        </div>

                        <div className="entity-card-footer">
                          <span className="reg-time">
                            {ev.collectedAt ? new Date(ev.collectedAt).toLocaleDateString() : 'Cataloged'}
                          </span>
                          
                          <div className="ev-card-actions">
                            <button 
                              className="btn-ev-action view"
                              onClick={() => handleOpenDetailModal(ev)}
                              title="Inspect Evidence Metadata & Verification Details"
                            >
                              <Eye size={13} />
                              <span>DETAILS</span>
                            </button>

                            <button 
                              className="btn-ev-action download"
                              onClick={() => handleDownload(ev)}
                              title="Secure Authenticated File Download"
                            >
                              <Download size={13} />
                              <span>DOWNLOAD</span>
                            </button>

                            <button 
                              className="btn-edit-subtle"
                              onClick={() => handleOpenEditEvidenceModal(ev)}
                              title="Edit Evidence Metadata"
                            >
                              <Edit3 size={13} />
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* TAB 6: CASE WITNESSES */}
          {activeTab === 'WITNESSES' && (
            <WitnessManagementTab
              caseId={id}
              witnesses={witnesses}
              loading={loadingWitnesses}
              onWitnessesUpdated={fetchWitnesses}
            />
          )}

          {/* TAB 7: AI INVESTIGATION */}
          {(activeTab === 'AI_INVESTIGATION' || activeTab === 'AI_SERVICE') && (
            <div className="space-y-6">
              <AIInvestigationPanel caseId={id} caseNumber={caseData?.caseNumber} />
            </div>
          )}

          {/* TAB 8: CHRONOLOGICAL CASE TIMELINE */}
          {activeTab === 'TIMELINE' && (
            <CaseTimelineTab
              timelineEvents={timelineEvents}
              loading={loadingTimeline}
              onRefresh={fetchTimeline}
            />
          )}

          {/* TAB: NOTIFICATIONS DISPATCH AUDIT */}
          {activeTab === 'NOTIFICATIONS' && (
            <div className="notif-tab-container space-y-6">
              <div className="cs-panel cs-panel-accent notif-main-panel">
                <div className="notif-header-row">
                  <div className="notif-title-group">
                    <div className="notif-title-icon-box">
                      <Bell size={20} />
                    </div>
                    <div>
                      <div className="notif-title">
                        <span>CASE ALERT & NOTIFICATION DISPATCH AUDIT</span>
                        <span className="notif-badge-phase">PHASE 13</span>
                      </div>
                      <div className="notif-subtitle">
                        Precinct-isolated audit log for Email (Spring Mail SMTP) and WhatsApp Cloud API dispatches for Case #{caseData?.caseNumber}.
                      </div>
                    </div>
                  </div>

                  <div className="notif-header-actions">
                    <button
                      onClick={fetchCaseNotifications}
                      disabled={loadingNotifications}
                      className="btn-notif-refresh"
                      title="Refresh notification log"
                    >
                      <RefreshCw size={13} className={loadingNotifications ? 'spin-icon' : ''} />
                      <span>{loadingNotifications ? 'REFRESHING...' : 'REFRESH'}</span>
                    </button>

                    <button
                      onClick={() => {
                        setTestAlertSuccess('');
                        setTestAlertError('');
                        setIsTestAlertModalOpen(true);
                      }}
                      className="btn-notif-trigger"
                    >
                      <Send size={13} />
                      <span>TRIGGER DEV TEST ALERT</span>
                    </button>
                  </div>
                </div>

                {/* KPI Metrics */}
                <div className="notif-kpi-grid">
                  <div className="notif-kpi-card">
                    <span className="notif-kpi-label">TOTAL ALERTS</span>
                    <span className="notif-kpi-val">{caseNotifications.length}</span>
                  </div>
                  <div className="notif-kpi-card kpi-card-delivered">
                    <span className="notif-kpi-label">DELIVERED / SENT</span>
                    <span className="notif-kpi-val val-green">
                      {caseNotifications.filter(n => n.status === 'SENT').length}
                    </span>
                  </div>
                  <div className="notif-kpi-card kpi-card-failed">
                    <span className="notif-kpi-label">FAILED DISPATCHES</span>
                    <span className="notif-kpi-val val-red">
                      {caseNotifications.filter(n => n.status === 'FAILED').length}
                    </span>
                  </div>
                  <div className="notif-kpi-card kpi-card-email">
                    <span className="notif-kpi-label">EMAIL (SMTP)</span>
                    <span className="notif-kpi-val val-blue">
                      {caseNotifications.filter(n => n.channel === 'EMAIL').length}
                    </span>
                  </div>
                  <div className="notif-kpi-card kpi-card-whatsapp">
                    <span className="notif-kpi-label">WHATSAPP CLOUD</span>
                    <span className="notif-kpi-val val-whatsapp">
                      {caseNotifications.filter(n => n.channel === 'WHATSAPP').length}
                    </span>
                  </div>
                </div>

                {/* Dispatch List */}
                {loadingNotifications ? (
                  <div className="notif-loading-box">
                    <RefreshCw size={18} className="spin-icon text-red" />
                    <span>FETCHING NOTIFICATION AUDIT LOG...</span>
                  </div>
                ) : caseNotifications.length === 0 ? (
                  <div className="notif-empty-box">
                    <Bell size={28} className="notif-empty-icon" />
                    <div className="notif-empty-title">No Dispatches Recorded For This Case</div>
                    <p className="notif-empty-desc">
                      Notifications are automatically dispatched on case creation, status/priority updates, AI review triggers, and investigator decisions.
                    </p>
                    <button
                      onClick={() => {
                        setTestAlertSuccess('');
                        setTestAlertError('');
                        setIsTestAlertModalOpen(true);
                      }}
                      className="btn-notif-empty-trigger"
                    >
                      <Send size={13} />
                      <span>Send Development Test Alert</span>
                    </button>
                  </div>
                ) : (
                  <div className="notif-cards-list">
                    {caseNotifications.map((notif) => (
                      <div key={notif.id} className="notif-dispatch-card">
                        <div className="notif-card-top">
                          <div className="notif-card-badges">
                            <span className={`channel-pill ${notif.channel === 'EMAIL' ? 'pill-email' : 'pill-whatsapp'}`}>
                              {notif.channel === 'EMAIL' ? <Mail size={11} /> : <MessageSquare size={11} />}
                              <span>{notif.channel}</span>
                            </span>

                            <span className="event-type-badge">
                              {notif.type}
                            </span>

                            {notif.recipient && (
                              <span className="recipient-badge">
                                → {notif.recipient}
                              </span>
                            )}
                          </div>

                          <div className="notif-card-status-row">
                            <span className={`status-pill pill-${(notif.status || '').toLowerCase()}`}>
                              {notif.status}
                            </span>

                            <span className="notif-timestamp">
                              {notif.createdAt ? new Date(notif.createdAt).toLocaleString() : ''}
                            </span>
                          </div>
                        </div>

                        {notif.subject && (
                          <div className="notif-subject-line">
                            {notif.subject}
                          </div>
                        )}

                        <div className="notif-body-preview">
                          {notif.content || notif.message}
                        </div>

                        {notif.channel === 'WHATSAPP' && (
                          <div style={{ marginTop: '8px' }}>
                            <button
                              type="button"
                              onClick={() => {
                                setSelectedWaNotif(notif);
                                setIsWaModalOpen(true);
                              }}
                              className="btn-open-source"
                              style={{ color: '#25D366', borderColor: '#25D366', background: 'rgba(37, 211, 102, 0.1)', cursor: 'pointer' }}
                            >
                              <MessageSquare size={12} />
                              <span>VIEW MOCK WHATSAPP PREVIEW</span>
                            </button>
                          </div>
                        )}

                        {notif.status === 'FAILED' && notif.errorMessage && (
                          <div className="notif-failure-banner">
                            <AlertTriangle size={14} className="failure-icon" />
                            <div>
                              <span className="failure-label">Dispatch Error: </span>
                              <span>{notif.errorMessage}</span>
                              {notif.retryCount !== undefined && (
                                <span className="retry-counter-tag">
                                  (Retries: {notif.retryCount} / 3)
                                </span>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB: PUBLIC NEWS & CRIME SEARCH INTELLIGENCE */}
          {activeTab === 'PUBLIC_INTEL' && (
            <div className="public-intel-tab-container space-y-6">
              {/* Mandatory Responsible AI / Legal Safety Warning Banner */}
              <div className="intel-disclaimer-banner cs-panel">
                <div className="disclaimer-header">
                  <ShieldAlert size={18} className="text-red" />
                  <span className="disclaimer-title">PUBLIC INFORMATION INTELLIGENCE — LEGAL & SAFETY ADVISORY</span>
                </div>
                <p className="disclaimer-text">
                  Public information is collected from external news and web sources and may be incomplete, outdated, inaccurate, or misleading. 
                  It must be independently verified before being relied upon in an investigation. <strong>Public news items are NOT official evidence</strong>, 
                  do not establish guilt or innocence, and are strictly separated from the evidence vault.
                </p>
                <div className="disclaimer-subtext">
                  CrimeSphere AI does not determine guilt or innocence from public information. Human investigator verification is required.
                </div>
              </div>

              {/* Main Search Panel */}
              <div className="cs-panel cs-panel-accent public-search-main-panel">
                <div className="intel-header-row">
                  <div className="intel-title-group">
                    <div className="intel-title-icon-box">
                      <Globe size={20} />
                    </div>
                    <div>
                      <div className="intel-title">
                        <span>PUBLIC CRIME & NEWS INTELLIGENCE SEARCH</span>
                        <span className="intel-badge-phase">PHASE 14</span>
                      </div>
                      <div className="intel-subtitle">
                        Search publicly available news articles related to Case #{caseData?.caseNumber || id}. All queries are station-authorized.
                      </div>
                    </div>
                  </div>

                  <div className="intel-header-actions">
                    <button
                      onClick={fetchSuggestedQueries}
                      disabled={loadingSuggestedQueries}
                      className="btn-intel-refresh"
                      title="Reload suggested queries"
                    >
                      <RefreshCw size={13} className={loadingSuggestedQueries ? 'spin-icon' : ''} />
                      <span>{loadingSuggestedQueries ? 'REFRESHING...' : 'RELOAD QUERIES'}</span>
                    </button>
                  </div>
                </div>

                {/* Case-Aware Suggested Queries Section */}
                <div className="suggested-queries-box">
                  <div className="suggested-label">
                    <Search size={12} />
                    <span>SUGGESTED CASE QUERIES (CLICK TO POPULATE):</span>
                  </div>
                  <div className="suggested-chips-wrap">
                    {suggestedQueries.length > 0 ? (
                      suggestedQueries.map((queryText, idx) => (
                        <button
                          key={idx}
                          type="button"
                          className="suggested-query-chip"
                          onClick={() => {
                            setPublicSearchQuery(queryText);
                            handleExecutePublicSearch(queryText);
                          }}
                        >
                          <span>{queryText}</span>
                        </button>
                      ))
                    ) : (
                      <span className="text-muted text-xs">No suggested queries generated for this case profile.</span>
                    )}
                  </div>
                </div>

                {/* Search Form */}
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleExecutePublicSearch();
                  }}
                  className="public-search-form"
                >
                  <div className="search-input-row">
                    <div className="search-query-wrapper">
                      <Search size={16} className="search-field-icon" />
                      <input
                        type="text"
                        value={publicSearchQuery}
                        onChange={(e) => setPublicSearchQuery(e.target.value)}
                        placeholder="Search public crime reports, police press releases, incidents (e.g., Coimbatore jewelry heist)..."
                        className="case-input search-text-input"
                        minLength={3}
                        maxLength={300}
                        required
                      />
                    </div>

                    <select
                      value={publicSearchType}
                      onChange={(e) => setPublicSearchType(e.target.value)}
                      className="case-select search-type-select"
                    >
                      <option value="NEWS">NEWS (Articles & Bulletins)</option>
                      <option value="WEB">WEB (General Search)</option>
                    </select>

                    <button
                      type="submit"
                      disabled={searchingPublic}
                      className="btn-intel-search"
                    >
                      {searchingPublic ? (
                        <>
                          <RefreshCw size={14} className="spin-icon" />
                          <span>SEARCHING...</span>
                        </>
                      ) : (
                        <>
                          <Search size={14} />
                          <span>SEARCH INTEL</span>
                        </>
                      )}
                    </button>
                  </div>

                  <div className="search-filters-row">
                    <div className="date-filter-group">
                      <span className="filter-label">FROM:</span>
                      <input
                        type="date"
                        value={publicSearchDateFrom}
                        onChange={(e) => setPublicSearchDateFrom(e.target.value)}
                        className="case-input date-field"
                      />
                    </div>
                    <div className="date-filter-group">
                      <span className="filter-label">TO:</span>
                      <input
                        type="date"
                        value={publicSearchDateTo}
                        onChange={(e) => setPublicSearchDateTo(e.target.value)}
                        className="case-input date-field"
                      />
                    </div>
                    <div className="text-muted text-[11px] ml-auto">
                      Rate limit: 20 searches/min • Results cached 5 mins
                    </div>
                  </div>
                </form>

                {publicSearchError && (
                  <div className="intel-error-banner">
                    <AlertTriangle size={15} />
                    <span>{publicSearchError}</span>
                  </div>
                )}

                {/* Live Search Results List */}
                {searchingPublic ? (
                  <div className="notif-loading-box">
                    <RefreshCw size={20} className="spin-icon text-red" />
                    <span>SEARCHING PUBLIC NEWS CHANNELS & PRESS RELEASES...</span>
                  </div>
                ) : (publicSearchResults || []).length > 0 && (
                  <div className="public-results-section">
                    <div className="results-section-header">
                      <div className="flex items-center gap-3 flex-wrap">
                        <span className="results-count-title">
                          PUBLIC SEARCH RESULTS ({(publicSearchResults || []).length} FOUND)
                        </span>
                        <span className="source-name-badge" style={{ background: 'rgba(59, 130, 246, 0.15)', borderColor: 'rgba(59, 130, 246, 0.4)', color: '#60A5FA' }}>
                          Provider: {publicSearchProvider ? (publicSearchProvider === 'newsapi' ? 'NEWS_API' : publicSearchProvider.toUpperCase()) : 'NEWS_API'}
                        </span>
                      </div>
                      <span className="results-warning-tag">
                        UNVERIFIED EXTERNAL SOURCES
                      </span>
                    </div>

                    <div className="results-cards-grid">
                      {(publicSearchResults || []).map((item, idx) => {
                        const isSaved = (savedPublicItems || []).some((s) => s?.sourceUrl?.trim()?.toLowerCase() === item?.url?.trim()?.toLowerCase());
                        const hasVideo = Boolean((item.videoUrl && isPlayableVideoUrl(item.videoUrl)) || (item.video && isPlayableVideoUrl(item.video)));
                        const mediaUrl = item.imageUrl || item.urlToImage;

                        return (
                          <article key={item.id || idx} className="public-news-card public-result-card cs-panel">
                            <div className="public-news-meta result-card-header">
                              <div className="public-news-source">
                                <span className="source-name-badge">{item.sourceName || 'News Source'}</span>
                              </div>
                              {item.publishedAt && (
                                <div className="public-news-date source-date">
                                  <Calendar size={11} className="inline mr-1 opacity-70" />
                                  <span>{new Date(item.publishedAt).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                                </div>
                              )}
                              {item.author && (
                                <div className="public-news-author source-author">
                                  <span>By {item.author}</span>
                                </div>
                              )}
                            </div>

                            <div className="public-news-content result-card-body">
                              <h3 className="public-news-title result-title">{item.title}</h3>
                              {item.description && (
                                <p className="public-news-description result-description">{item.description}</p>
                              )}

                              {hasVideo ? (
                                <div className="public-news-media">
                                  <video
                                    className="public-news-video"
                                    controls
                                    preload="metadata"
                                    playsInline
                                  >
                                    <source src={item.videoUrl || item.video} type="video/mp4" />
                                    Your browser does not support video playback.
                                  </video>
                                </div>
                              ) : mediaUrl ? (
                                <div className="public-news-media result-img-wrapper">
                                  <img
                                    src={mediaUrl}
                                    alt={item.title || "Public news thumbnail"}
                                    className="result-thumbnail"
                                    loading="lazy"
                                    onError={(e) => {
                                      e.currentTarget.style.display = 'none';
                                    }}
                                  />
                                </div>
                              ) : (
                                <div className="public-news-media public-news-no-media">
                                  <div className="no-media-placeholder">
                                    <ImageIcon size={18} />
                                    <span>No media available</span>
                                  </div>
                                </div>
                              )}

                              <div className="public-news-verification unverified-chip">
                                PUBLIC INFORMATION — REQUIRES VERIFICATION
                              </div>

                              <div className="public-news-actions result-card-actions">
                                {isMockPublicUrl(item.url, item.sourceName, item.title, publicSearchProvider || item.relevanceIndicator) ? (
                                  <span className="btn-open-source mock-disabled" title="Development mock data — no external source available">
                                    <Info size={13} />
                                    <span>Development mock data — no external source available</span>
                                  </span>
                                ) : (
                                  <a
                                    href={item.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="btn-open-source"
                                  >
                                    <ExternalLink size={13} />
                                    <span>READ ARTICLE</span>
                                  </a>
                                )}

                                <button
                                  type="button"
                                  onClick={() => handleSavePublicItem(item)}
                                  disabled={isSaved || savingItemUrl === item.url}
                                  className={`btn-save-public ${isSaved ? 'saved' : ''}`}
                                >
                                  {isSaved ? (
                                    <>
                                      <BookmarkCheck size={13} />
                                      <span>BOOKMARKED TO CASE</span>
                                    </>
                                  ) : savingItemUrl === item.url ? (
                                    <>
                                      <RefreshCw size={13} className="spin-icon" />
                                      <span>SAVING...</span>
                                    </>
                                  ) : (
                                    <>
                                      <BookmarkPlus size={13} />
                                      <span>SAVE TO CASE</span>
                                    </>
                                  )}
                                </button>
                              </div>
                            </div>
                          </article>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>

              {/* Saved Public Information Section */}
              <div className="cs-panel cs-panel-elevated saved-intel-panel">
                <div className="saved-intel-header">
                  <div className="saved-intel-title-row">
                    <BookmarkCheck size={18} className="text-red flex-shrink-0" />
                    <div className="saved-intel-heading">
                      <span className="saved-title-text">SAVED PUBLIC INTELLIGENCE RECORDS</span>
                      <span className="saved-count-badge">{savedPublicItems.length} RECORDED</span>
                    </div>
                  </div>
                  <button
                    onClick={fetchSavedPublicItems}
                    disabled={loadingSavedPublicItems}
                    className="btn-intel-refresh"
                  >
                    <RefreshCw size={13} className={loadingSavedPublicItems ? 'spin-icon' : ''} />
                    <span>REFRESH</span>
                  </button>
                </div>

                {loadingSavedPublicItems ? (
                  <div className="notif-loading-box">
                    <RefreshCw size={18} className="spin-icon text-red" />
                    <span>FETCHING SAVED CASE INTELLIGENCE...</span>
                  </div>
                ) : savedPublicItems.length === 0 ? (
                  <div className="notif-empty-box">
                    <Globe size={28} className="notif-empty-icon" />
                    <div className="notif-empty-title">No Public Information Saved Yet</div>
                    <p className="notif-empty-desc">
                      Search above for news reports or press releases relevant to this case and click <strong>SAVE TO CASE</strong> to bookmark them for investigator review.
                    </p>
                  </div>
                ) : (
                  <div className="saved-items-list">
                    {savedPublicItems.map((saved) => (
                      <div key={saved.id} className="saved-item-card cs-panel">
                        <div className="saved-item-top">
                          <div className="saved-item-meta">
                            <span className="source-name-badge">{saved.sourceName || 'News Source'}</span>
                            <span className={`intel-status-pill status-${(saved.verificationStatus || 'UNVERIFIED').toLowerCase()}`}>
                              {saved.verificationStatus}
                            </span>
                            {saved.publishedAt && (
                              <span className="source-date">
                                Published: {new Date(saved.publishedAt).toLocaleDateString()}
                              </span>
                            )}
                          </div>

                          <div className="saved-item-actions">
                            {isMockPublicUrl(saved.sourceUrl, saved.sourceName, saved.title) ? (
                              <span className="btn-saved-open mock-disabled" title="Development mock data — no external source available">
                                <Info size={13} />
                                <span>Development mock data — no external source available</span>
                              </span>
                            ) : (
                              <a
                                href={saved.sourceUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="btn-saved-open"
                                title="Open original publication link"
                              >
                                <ExternalLink size={13} />
                                <span>VIEW SOURCE</span>
                              </a>
                            )}

                            <button
                              type="button"
                              onClick={() => handleOpenReviewModal(saved)}
                              className="btn-saved-review"
                            >
                              <Edit3 size={13} />
                              <span>REVIEW & VERIFY</span>
                            </button>

                            <button
                              type="button"
                              onClick={() => handleDeletePublicItem(saved.id)}
                              className="btn-saved-delete"
                              title="Delete record from case audit"
                            >
                              <Trash2 size={13} />
                            </button>
                          </div>
                        </div>

                        <div className="saved-item-body">
                          <h4 className="saved-item-title">{saved.title}</h4>
                          {saved.description && (
                            <p className="saved-item-desc">{saved.description}</p>
                          )}
                          {saved.notes && (
                            <div className="investigator-notes-box">
                              <span className="notes-label">INVESTIGATOR NOTES:</span>
                              <p className="notes-text">{saved.notes}</p>
                            </div>
                          )}
                        </div>

                        <div className="saved-item-footer">
                          <span>Saved by: {saved.savedByOfficerName || 'Station Investigator'}</span>
                          <span>Recorded: {saved.createdAt ? new Date(saved.createdAt).toLocaleString() : ''}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 10: GENERATE REPORT */}
          {activeTab === 'GENERATE_REPORT' && (
            <div className="report-tab-container space-y-6">
              <div className="cs-panel cs-panel-accent report-gen-panel">
                <div className="report-hero">
                  <div className="report-hero-icon-wrap">
                    <FileCheck size={28} className="text-crimson" />
                  </div>
                  <div className="report-hero-info">
                    <h2 className="report-hero-title">OFFICIAL INVESTIGATION DOSSIER REPORT</h2>
                    <p className="report-hero-desc">
                      Compile and generate a cryptographically verifiable police investigation report in PDF format containing complete FIR record, victim details, suspect profiles, witness testimonies, evidence chain-of-custody, and AI intelligence analysis.
                    </p>
                  </div>
                  <button 
                    className="btn-download-pdf-large"
                    onClick={handleGeneratePdfReport}
                    disabled={generatingPdf}
                  >
                    <Download size={16} className={generatingPdf ? 'spin-icon' : ''} />
                    <span>{generatingPdf ? 'COMPILING PDF DOSSIER...' : 'EXPORT OFFICIAL PDF REPORT'}</span>
                  </button>
                </div>

                {pdfError && (
                  <div className="form-error-banner mt-4">{pdfError}</div>
                )}

                {/* Case Report Summary Matrix */}
                <div className="report-summary-grid mt-6">
                  <div className="report-summary-card">
                    <span className="sum-label">CASE NUMBER</span>
                    <span className="sum-val text-red">{caseData?.caseNumber || 'N/A'}</span>
                    <span className="sum-sub">{caseData?.priority || 'NORMAL'} PRIORITY • {(caseData?.status || '').replace(/_/g, ' ')}</span>
                  </div>
                  <div className="report-summary-card">
                    <span className="sum-label">FIR STATUS</span>
                    <span className="sum-val text-amber">{firData ? firData.firNumber : 'UNREGISTERED'}</span>
                    <span className="sum-sub">{firData ? firData.sectionsOfLaw || 'Sections Filed' : 'Pending Filing'}</span>
                  </div>
                  <div className="report-summary-card">
                    <span className="sum-label">PERSONS CATALOGED</span>
                    <span className="sum-val text-purple">{victims.length + suspects.length + witnesses.length}</span>
                    <span className="sum-sub">{victims.length} Victims • {suspects.length} Suspects • {witnesses.length} Witnesses</span>
                  </div>
                  <div className="report-summary-card">
                    <span className="sum-label">EVIDENCE VAULT</span>
                    <span className="sum-val text-green">{evidenceList.length} Items</span>
                    <span className="sum-sub">{evidenceList.filter(e => e.status === 'VERIFIED').length} Verified SHA-256</span>
                  </div>
                </div>

                {/* Past Reports History */}
                <div className="report-history-section mt-6">
                  <div className="history-header">
                    <History size={16} className="text-muted" />
                    <span className="history-title">REPORT GENERATION AUDIT LOGS</span>
                  </div>
                  {reportHistory && reportHistory.length > 0 ? (
                    <div className="history-table-wrap mt-3">
                      <table className="history-table">
                        <thead>
                          <tr>
                            <th>GENERATED AT</th>
                            <th>OFFICER</th>
                            <th>FORMAT</th>
                            <th>STATUS</th>
                            <th>ACTION</th>
                          </tr>
                        </thead>
                        <tbody>
                          {reportHistory.map((rep, idx) => (
                            <tr key={rep.id || idx}>
                              <td>{new Date(rep.createdAt || rep.timestamp).toLocaleString()}</td>
                              <td>{rep.generatedBy?.fullName || 'Station Officer'}</td>
                              <td>PDF DOSSIER</td>
                              <td><span className="status-badge badge-success">GENERATED</span></td>
                              <td>
                                <button className="btn-re-download" onClick={handleGeneratePdfReport}>
                                  DOWNLOAD AGAIN
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="history-empty-note mt-3">
                      <span>No previous PDF generation logs found for this case. Click above to compile and export the official dossier.</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 11: AI SCENE ILLUSTRATIONS & RECONSTRUCTION */}
          {activeTab === 'AI_ILLUSTRATIONS' && (
            <div className="illustrations-tab-container space-y-6">
              {/* Header Panel */}
              <div className="cs-panel cs-panel-accent illustration-hero-panel">
                <div className="illustration-hero-header">
                  <div className="illustration-hero-title-group">
                    <div className="illustration-title-icon-box">
                      <Sparkles size={22} className="text-purple" />
                    </div>
                    <div>
                      <div className="illustration-header-badge-row">
                        <h2 className="illustration-section-title">AI INVESTIGATION SCENE VISUALIZATION & RECONSTRUCTION</h2>
                        <span className="badge-ai-model-tag">GEMINI VISION AI</span>
                      </div>
                      <p className="illustration-section-subtitle">
                        Generate synthetic scene illustrations and situational reconstructions based on case narrative, witness testimonies, and spatial hypotheses.
                      </p>
                    </div>
                  </div>

                  <button
                    onClick={fetchIllustrations}
                    disabled={loadingIllustrations}
                    className="btn-notif-refresh"
                    title="Refresh AI illustrations catalog"
                  >
                    <RefreshCw size={13} className={loadingIllustrations ? 'spin-icon' : ''} />
                    <span>{loadingIllustrations ? 'REFRESHING...' : 'REFRESH'}</span>
                  </button>
                </div>

                {/* MANDATORY RESPONSIBLE AI EVIDENTIARY WARNING */}
                <div className="ai-evidentiary-warning-box">
                  <div className="warning-box-header">
                    <ShieldAlert size={18} className="text-crimson" />
                    <span className="warning-box-title">CRITICAL EVIDENTIARY DISCLAIMER: SYNTHETIC VISUAL AID</span>
                  </div>
                  <p className="warning-box-body">
                    <strong>AI-GENERATED ILLUSTRATION — NOT EVIDENCE.</strong> These images are synthesized through generative AI algorithms for investigative visualization and hypothesis modeling only. They are <strong>NOT</strong> real crime-scene photographs, <strong>NOT</strong> forensic proof, and <strong>DO NOT</strong> identify actual individuals or confirm biometric identity. Synthetic visuals must never be presented as verified evidence in court or official records.
                  </p>
                </div>

                {/* Generation Form Builder */}
                <form onSubmit={handleGenerateIllustration} className="illustration-form-builder">
                  <div className="form-group">
                    <div className="prompt-label-row">
                      <label className="form-label label-system">
                        SCENE RECONSTRUCTION PROMPT <span className="req">*</span>
                      </label>
                      <span className="prompt-hint">Describe scene elements, lighting, camera vantage, weather, and key objects</span>
                    </div>

                    <textarea
                      value={illustrationPrompt}
                      onChange={(e) => setIllustrationPrompt(e.target.value)}
                      placeholder="e.g. Fictional nighttime street scene showing a dark sedan parked near an industrial warehouse loading dock, yellow sodium vapor streetlamp casting long diagonal shadows on wet pavement..."
                      rows={4}
                      className="case-textarea prompt-textarea"
                      disabled={generatingIllustration}
                      required
                    />
                  </div>

                  {/* Preset Prompt Inspiration Chips */}
                  <div className="prompt-suggestions-container">
                    <span className="suggestions-label">SAMPLE INVESTIGATIVE PROMPTS:</span>
                    <div className="suggestions-chips-grid">
                      {[
                        `Nighttime industrial alleyway showing loading dock entrance with single security floodlight and tyre skid marks on asphalt`,
                        `Floorplan layout perspective of commercial retail storefront with shattered glass entrance and streetlamp reflection`,
                        `Residential suburban driveway at twilight with parked silver SUV and overhead porch light casting directional illumination`,
                        `Overhead birds-eye vantage point of multi-lane urban intersection with traffic signal infrastructure and crosswalk demarcations`
                      ].map((preset, idx) => (
                        <button
                          key={idx}
                          type="button"
                          className="prompt-preset-chip"
                          disabled={generatingIllustration}
                          onClick={() => setIllustrationPrompt(preset)}
                        >
                          <Wand2 size={11} />
                          <span className="preset-text">{preset}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Controls & Submit */}
                  <div className="illustration-controls-row">
                    <div className="aspect-ratio-selector-group">
                      <span className="aspect-ratio-label">ASPECT RATIO:</span>
                      <div className="aspect-ratio-buttons">
                        {[
                          { id: '1:1', label: '1:1 SQUARE', desc: '1024x1024' },
                          { id: '16:9', label: '16:9 WIDE', desc: '1344x768' },
                          { id: '4:3', label: '4:3 STANDARD', desc: '1184x864' }
                        ].map((ar) => (
                          <button
                            key={ar.id}
                            type="button"
                            className={`ar-btn ${illustrationAspectRatio === ar.id ? 'active' : ''}`}
                            onClick={() => setIllustrationAspectRatio(ar.id)}
                            disabled={generatingIllustration}
                          >
                            <span className="ar-main-text">{ar.label}</span>
                            <span className="ar-sub-text">{ar.desc}</span>
                          </button>
                        ))}
                      </div>
                    </div>

                    <button
                      type="submit"
                      className="btn-generate-ai-illustration"
                      disabled={generatingIllustration || !illustrationPrompt.trim()}
                    >
                      {generatingIllustration ? (
                        <>
                          <RefreshCw size={15} className="spin-icon" />
                          <span>SYNTHESIZING SCENE WITH GEMINI...</span>
                        </>
                      ) : (
                        <>
                          <Sparkles size={15} />
                          <span>GENERATE SCENE ILLUSTRATION</span>
                        </>
                      )}
                    </button>
                  </div>

                  {/* Feedback Banners */}
                  {illustrationError && (
                    <div className="notif-modal-feedback error mt-3">
                      <AlertTriangle size={15} />
                      <span>{illustrationError}</span>
                    </div>
                  )}

                  {illustrationSuccess && (
                    <div className="notif-modal-feedback success mt-3">
                      <CheckCircle size={15} />
                      <span>{illustrationSuccess}</span>
                    </div>
                  )}
                </form>
              </div>

              {/* Gallery of Generated Scene Visualizations */}
              <div className="illustrations-gallery-section">
                <div className="gallery-header-row">
                  <div className="gallery-header-left">
                    <HardDrive size={16} className="text-muted" />
                    <span className="gallery-title">GENERATED SCENE RECONSTRUCTIONS</span>
                    <span className="gallery-count-badge">{illustrations.length} ITEMS</span>
                  </div>
                  <span className="gallery-disclaimer-tag">NON-EVIDENTIARY ARCHIVE</span>
                </div>

                {loadingIllustrations ? (
                  <div className="notif-loading-box">
                    <RefreshCw size={20} className="spin-icon text-purple" />
                    <span>LOADING AI SCENE ILLUSTRATIONS...</span>
                  </div>
                ) : illustrations.length === 0 ? (
                  <div className="entity-empty-card cs-panel">
                    <Sparkles size={40} className="empty-mod-icon text-purple" />
                    <div className="empty-title">NO SCENE ILLUSTRATIONS GENERATED YET</div>
                    <div className="empty-desc">
                      Create hypothesis illustrations using the Gemini prompt builder above to visualize vantage points, scene arrangements, and lighting conditions.
                    </div>
                  </div>
                ) : (
                  <div className="illustrations-grid">
                    {illustrations.map((item) => {
                      const illUrl = item.viewUrl ? resolveAssetUrl(item.viewUrl) : getIllustrationViewUrl(id, item.id);
                      return (
                        <div key={item.id} className="illustration-card cs-panel cs-panel-accent">
                          {/* Image Container with Badges */}
                          <div 
                            className="illustration-img-container"
                            onClick={() => setActiveLightboxImage({
                              url: illUrl,
                              title: `AI SCENE RECONSTRUCTION: "${item.prompt.substring(0, 50)}..."`,
                              badge: 'AI-GENERATED ILLUSTRATION — NOT EVIDENCE',
                              badgeType: 'ai',
                              subtitle: `Prompt: ${item.prompt} • Generated: ${new Date(item.createdAt).toLocaleString()} • Ratio: ${item.aspectRatio || '1:1'} • Model: ${item.modelUsed || 'Gemini Vision'}`
                            })}
                            title="Click to view high-resolution image"
                          >
                            <img
                              src={illUrl}
                              alt={item.prompt}
                              className="illustration-card-img"
                              onError={(e) => {
                                e.target.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"><rect fill="%23131620" width="400" height="300"/><text fill="%23ef4444" x="50%25" y="50%25" text-anchor="middle" font-family="sans-serif" font-size="14">AI Image Render Offline</text></svg>';
                              }}
                            />
                            
                            {/* Mandatory Distinct Badges */}
                            <div className="badge-ai-illustration-overlay">
                              <ShieldAlert size={10} />
                              <span>AI-GENERATED ILLUSTRATION — NOT EVIDENCE</span>
                            </div>

                            <div className="badge-ai-source-tag">
                              <Sparkles size={10} />
                              <span>Generated by AI</span>
                            </div>
                          </div>

                          {/* Card Details */}
                          <div className="illustration-card-body">
                            <div className="illustration-prompt-box">
                              <span className="prompt-quote-label">PROMPT:</span>
                              <p className="illustration-prompt-text">{item.prompt}</p>
                            </div>

                            <div className="illustration-meta-grid">
                              <div className="ill-meta-item">
                                <span className="meta-lbl">MODEL:</span>
                                <span className="meta-val">{item.modelUsed || 'Gemini Vision'}</span>
                              </div>
                              <div className="ill-meta-item">
                                <span className="meta-lbl">ASPECT:</span>
                                <span className="meta-val">{item.aspectRatio || '1:1'}</span>
                              </div>
                              <div className="ill-meta-item">
                                <span className="meta-lbl">OFFICER:</span>
                                <span className="meta-val">{item.generatedBy?.fullName || 'Station Officer'}</span>
                              </div>
                              <div className="ill-meta-item">
                                <span className="meta-lbl">CREATED:</span>
                                <span className="meta-val">{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Recent'}</span>
                              </div>
                            </div>
                          </div>

                          {/* Card Actions */}
                          <div className="illustration-card-footer">
                            <button
                              className="btn-ill-action view"
                              onClick={() => setActiveLightboxImage({
                                url: illUrl,
                                title: `AI SCENE RECONSTRUCTION: "${item.prompt.substring(0, 50)}..."`,
                                badge: 'AI-GENERATED ILLUSTRATION — NOT EVIDENCE',
                                badgeType: 'ai',
                                subtitle: `Prompt: ${item.prompt} • Generated: ${new Date(item.createdAt).toLocaleString()} • Ratio: ${item.aspectRatio || '1:1'} • Model: ${item.modelUsed || 'Gemini Vision'}`
                              })}
                            >
                              <Maximize2 size={12} />
                              <span>INSPECT</span>
                            </button>

                            <a
                              href={illUrl}
                              download={`case-${caseData?.caseNumber || 'case'}-ai-illustration-${item.id.substring(0, 8)}.jpg`}
                              className="btn-ill-action download"
                              title="Download synthetic visual file"
                              target="_blank"
                              rel="noreferrer"
                            >
                              <Download size={12} />
                              <span>DOWNLOAD</span>
                            </a>

                            <button
                              className="btn-ill-action delete"
                              onClick={() => handleDeleteIllustration(item.id)}
                              title="Delete this synthetic illustration"
                            >
                              <Trash2 size={12} />
                              <span>DELETE</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* MODAL 1: EDIT CASE OVERVIEW */}
          {isEditCaseModalOpen && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <Edit3 size={18} className="modal-title-icon" />
                    <span>MODIFY INVESTIGATION RECORD — {caseData.caseNumber}</span>
                  </div>
                  <button className="btn-close-modal" onClick={() => setIsEditCaseModalOpen(false)}>
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSaveFullCaseEdit} className="modal-form">
                  <div className="form-group">
                    <label className="form-label label-system">CASE TITLE</label>
                    <input 
                      type="text" 
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      className="case-input"
                      required
                    />
                  </div>

                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">CASE TYPE</label>
                      <select 
                        value={editType}
                        onChange={(e) => setEditType(e.target.value)}
                        className="case-select"
                      >
                        <option value="MURDER">MURDER</option>
                        <option value="THEFT">THEFT</option>
                        <option value="MISSING_PERSON">MISSING PERSON</option>
                        <option value="ASSAULT">ASSAULT</option>
                        <option value="CYBER_CRIME">CYBER CRIME</option>
                        <option value="OTHER">OTHER</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">PRIORITY</label>
                      <select 
                        value={editPriority}
                        onChange={(e) => setEditPriority(e.target.value)}
                        className="case-select"
                      >
                        <option value="CRITICAL">CRITICAL</option>
                        <option value="HIGH">HIGH</option>
                        <option value="MEDIUM">MEDIUM</option>
                        <option value="LOW">LOW</option>
                      </select>
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">INVESTIGATION STATUS</label>
                    <select 
                      value={editStatus}
                      onChange={(e) => setEditStatus(e.target.value)}
                      className="case-select"
                    >
                      <option value="OPEN">OPEN</option>
                      <option value="UNDER_INVESTIGATION">UNDER INVESTIGATION</option>
                      <option value="PENDING_REVIEW">PENDING REVIEW</option>
                      <option value="ON_HOLD">ON HOLD</option>
                      <option value="CLOSED">CLOSED</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">NARRATIVE & BRIEFING</label>
                    <textarea 
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      rows={5}
                      className="case-textarea"
                    />
                  </div>

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsEditCaseModalOpen(false)}
                      disabled={savingCase}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={savingCase}
                    >
                      <Save size={15} />
                      <span>{savingCase ? 'SAVING CHANGES...' : 'SAVE MODIFICATIONS'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL 2: REGISTER / EDIT FIR */}
          {isFirModalOpen && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <FileBadge size={18} className="modal-title-icon" />
                    <span>{isEditingFir ? `EDIT FIR — ${firData?.firNumber}` : `REGISTER FIR FOR ${caseData.caseNumber}`}</span>
                  </div>
                  <button className="btn-close-modal" onClick={() => setIsFirModalOpen(false)}>
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSaveFir} className="modal-form">
                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">COMPLAINANT FULL NAME</label>
                      <input 
                        type="text" 
                        placeholder="e.g. S. Ramanathan (Park Inspector)"
                        value={firComplainant}
                        onChange={(e) => setFirComplainant(e.target.value)}
                        className="case-input"
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">COMPLAINANT CONTACT PHONE</label>
                      <input 
                        type="text" 
                        placeholder="e.g. 9876543210"
                        value={firContact}
                        onChange={(e) => setFirContact(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">REGISTRATION DATE/TIME</label>
                      <input 
                        type="datetime-local" 
                        value={firRegDate}
                        onChange={(e) => setFirRegDate(e.target.value)}
                        className="case-input"
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">INCIDENT DATE/TIME</label>
                      <input 
                        type="datetime-local" 
                        value={firIncDate}
                        onChange={(e) => setFirIncDate(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">INCIDENT LOCATION / SCENE OF CRIME</label>
                    <input 
                      type="text" 
                      placeholder="e.g. Near North Gate Jogging Track, Gandhi Park, Coimbatore"
                      value={firLocation}
                      onChange={(e) => setFirLocation(e.target.value)}
                      className="case-input"
                    />
                  </div>

                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">OFFENCE CLASSIFICATION</label>
                      <input 
                        type="text" 
                        placeholder="e.g. HOMICIDE"
                        value={firOffenceType}
                        onChange={(e) => setFirOffenceType(e.target.value)}
                        className="case-input"
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">SECTIONS OF LAW</label>
                      <input 
                        type="text" 
                        placeholder="e.g. IPC 302, IPC 34"
                        value={firSections}
                        onChange={(e) => setFirSections(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">OFFICIAL FIR DESCRIPTION & STATEMENT</label>
                    <textarea 
                      placeholder="Full summary of complainant statement, discovered circumstances, and initial police actions."
                      value={firDesc}
                      onChange={(e) => setFirDesc(e.target.value)}
                      rows={5}
                      className="case-textarea"
                    />
                  </div>

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsFirModalOpen(false)}
                      disabled={savingFir}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={savingFir}
                    >
                      <Save size={15} />
                      <span>{savingFir ? 'SAVING FIR...' : (isEditingFir ? 'UPDATE FIR' : 'REGISTER FIR')}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL 3: ADD / EDIT VICTIM */}
          {isVictimModalOpen && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <UserX size={18} className="modal-title-icon" />
                    <span>{editingVictimId ? 'EDIT VICTIM RECORD' : 'REGISTER CASE VICTIM'}</span>
                  </div>
                  <button className="btn-close-modal" onClick={() => setIsVictimModalOpen(false)}>
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSaveVictim} className="modal-form">
                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">FULL NAME <span className="req">*</span></label>
                      <input 
                        type="text" 
                        placeholder="e.g. K. Senthil Kumar"
                        value={vFullName}
                        onChange={(e) => setVFullName(e.target.value)}
                        className="case-input"
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">RELATIONSHIP TO CASE</label>
                      <input 
                        type="text" 
                        placeholder="e.g. Primary Victim, Eyewitness/Victim"
                        value={vRelationship}
                        onChange={(e) => setVRelationship(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-row-grid-3">
                    <div className="form-group">
                      <label className="form-label label-system">AGE</label>
                      <input 
                        type="number" 
                        placeholder="e.g. 42"
                        value={vAge}
                        onChange={(e) => setVAge(e.target.value)}
                        className="case-input"
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">GENDER</label>
                      <select 
                        value={vGender}
                        onChange={(e) => setVGender(e.target.value)}
                        className="case-select"
                      >
                        <option value="MALE">MALE</option>
                        <option value="FEMALE">FEMALE</option>
                        <option value="OTHER">OTHER</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">STATUS</label>
                      <select 
                        value={vStatus}
                        onChange={(e) => setVStatus(e.target.value)}
                        className="case-select"
                      >
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="DECEASED">DECEASED</option>
                        <option value="MISSING">MISSING</option>
                        <option value="IDENTIFIED">IDENTIFIED</option>
                      </select>
                    </div>
                  </div>

                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">PHONE NUMBER</label>
                      <input 
                        type="text" 
                        placeholder="e.g. 9843211234"
                        value={vPhone}
                        onChange={(e) => setVPhone(e.target.value)}
                        className="case-input"
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">OCCUPATION</label>
                      <input 
                        type="text" 
                        placeholder="e.g. Textile Warehouse Manager"
                        value={vOccupation}
                        onChange={(e) => setVOccupation(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">RESIDENCE ADDRESS</label>
                    <input 
                      type="text" 
                      placeholder="e.g. No 45, Cross Cut Road, Gandhipuram, Coimbatore"
                      value={vAddress}
                      onChange={(e) => setVAddress(e.target.value)}
                      className="case-input"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">VICTIM STATEMENT / INJURY REPORT</label>
                    <textarea 
                      placeholder="Observations on condition, injury documentation, or recovered belongings."
                      value={vDescription}
                      onChange={(e) => setVDescription(e.target.value)}
                      rows={4}
                      className="case-textarea"
                    />
                  </div>

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsVictimModalOpen(false)}
                      disabled={savingVictim}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={savingVictim}
                    >
                      <Save size={15} />
                      <span>{savingVictim ? 'SAVING...' : (editingVictimId ? 'UPDATE VICTIM' : 'REGISTER VICTIM')}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL 4: ADD / EDIT SUSPECT */}
          {isSuspectModalOpen && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <Users size={18} className="modal-title-icon" />
                    <span>{editingSuspectId ? 'EDIT SUSPECT RECORD' : 'REGISTER PERSON OF INTEREST / SUSPECT'}</span>
                  </div>
                  <button className="btn-close-modal" onClick={() => setIsSuspectModalOpen(false)}>
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSaveSuspect} className="modal-form">
                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">FULL NAME <span className="req">*</span></label>
                      <input 
                        type="text" 
                        placeholder="e.g. M. Rajesh"
                        value={sFullName}
                        onChange={(e) => setSFullName(e.target.value)}
                        className="case-input"
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">KNOWN ALIAS / MONIKER</label>
                      <input 
                        type="text" 
                        placeholder="e.g. Snake Rajesh"
                        value={sAlias}
                        onChange={(e) => setSAlias(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-row-grid-3">
                    <div className="form-group">
                      <label className="form-label label-system">AGE</label>
                      <input 
                        type="number" 
                        placeholder="e.g. 31"
                        value={sAge}
                        onChange={(e) => setSAge(e.target.value)}
                        className="case-input"
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">GENDER</label>
                      <select 
                        value={sGender}
                        onChange={(e) => setSGender(e.target.value)}
                        className="case-select"
                      >
                        <option value="MALE">MALE</option>
                        <option value="FEMALE">FEMALE</option>
                        <option value="OTHER">OTHER</option>
                      </select>
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">INVESTIGATION STATUS</label>
                      <select 
                        value={sStatus}
                        onChange={(e) => setSStatus(e.target.value)}
                        className="case-select"
                      >
                        <option value="UNDER_INVESTIGATION">UNDER INVESTIGATION</option>
                        <option value="PERSON_OF_INTEREST">PERSON OF INTEREST</option>
                        <option value="CLEARED">CLEARED</option>
                        <option value="REFERRED">REFERRED</option>
                      </select>
                    </div>
                  </div>

                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">PHONE NUMBER</label>
                      <input 
                        type="text" 
                        placeholder="e.g. 9789012345"
                        value={sPhone}
                        onChange={(e) => setSPhone(e.target.value)}
                        className="case-input"
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">KNOWN OCCUPATION</label>
                      <input 
                        type="text" 
                        placeholder="e.g. Former Warehouse Loader"
                        value={sOccupation}
                        onChange={(e) => setSOccupation(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">KNOWN RESIDENCE / HABITAT</label>
                    <input 
                      type="text" 
                      placeholder="e.g. Sector 8 Slum Board Tenements, Ukkadam, Coimbatore"
                      value={sAddress}
                      onChange={(e) => setSAddress(e.target.value)}
                      className="case-input"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">LEADS, STATEMENTS & ALIBI MATRIX</label>
                    <textarea 
                      placeholder="Investigative notes, observed motives, alibi statements, and tracking status."
                      value={sDescription}
                      onChange={(e) => setSDescription(e.target.value)}
                      rows={4}
                      className="case-textarea"
                    />
                  </div>

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsSuspectModalOpen(false)}
                      disabled={savingSuspect}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={savingSuspect}
                    >
                      <Save size={15} />
                      <span>{savingSuspect ? 'SAVING...' : (editingSuspectId ? 'UPDATE SUSPECT' : 'REGISTER SUSPECT')}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL 5: PHASE 6 UPLOAD EVIDENCE */}
          {isUploadEvidenceModalOpen && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent upload-evidence-modal">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <UploadCloud size={18} className="modal-title-icon" />
                    <span>SECURE EVIDENCE UPLOAD — {caseData.caseNumber}</span>
                  </div>
                  <button 
                    className="btn-close-modal" 
                    onClick={() => !uploadingEvidence && setIsUploadEvidenceModalOpen(false)}
                    disabled={uploadingEvidence}
                  >
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleUploadEvidenceSubmit} className="modal-form">
                  {uploadError && (
                    <div className="modal-error-alert">
                      <AlertTriangle size={16} />
                      <span>{uploadError}</span>
                    </div>
                  )}

                  {crossCaseWarning && (
                    <div className="modal-warning-alert cs-panel-accent" style={{ background: '#2a1a08', border: '1px solid #ff9800', color: '#ffb74d', padding: '16px', borderRadius: '8px', marginBottom: '16px' }}>
                      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                        <AlertTriangle size={24} style={{ color: '#ff9800', flexShrink: 0, marginTop: '2px' }} />
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 'bold', fontSize: '0.95rem', color: '#ffa726', marginBottom: '4px' }}>
                            CROSS-CASE EVIDENCE DETECTED
                          </div>
                          <div style={{ fontSize: '0.875rem', color: '#ffe0b2', lineHeight: '1.4' }}>
                            This file already exists in another case: <strong>{crossCaseWarning.caseNumber}</strong>. It may be relevant to multiple investigations.
                          </div>
                          <div style={{ display: 'flex', gap: '12px', marginTop: '14px' }}>
                            <button
                              type="button"
                              className="btn-cancel"
                              onClick={() => setCrossCaseWarning(null)}
                              disabled={uploadingEvidence}
                              style={{ padding: '6px 16px', fontSize: '0.8rem' }}
                            >
                              CANCEL
                            </button>
                            <button
                              type="button"
                              className="btn-save-edit"
                              onClick={(e) => handleUploadEvidenceSubmit(e, true)}
                              disabled={uploadingEvidence}
                              style={{ padding: '6px 16px', fontSize: '0.8rem', backgroundColor: '#e53935', borderColor: '#ef5350' }}
                            >
                              CONTINUE UPLOAD
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Drag & Drop / File Select Zone */}
                  <div className="file-drop-zone">
                    <input 
                      type="file" 
                      id="evidence-file-input"
                      onChange={handleFileSelection}
                      disabled={uploadingEvidence}
                      className="file-hidden-input"
                    />
                    <label htmlFor="evidence-file-input" className="file-drop-label">
                      <UploadCloud size={32} className="drop-icon" />
                      <div className="drop-title">
                        {selectedFile ? selectedFile.name : 'CLICK TO SELECT EVIDENCE FILE OR DRAG HERE'}
                      </div>
                      <div className="drop-subtitle">
                        {selectedFile 
                          ? `Size: ${formatBytes(selectedFile.size)} • Type: ${selectedFile.type || 'Standard File'}`
                          : 'Allowed: PDF, JPG, PNG, WEBP, MP4, MOV, MP3, WAV, DOC, DOCX, XLS, XLSX, TXT (Max: 50 MB)'}
                      </div>
                    </label>
                  </div>

                  {/* Image Preview for Evidence Photos */}
                  {evidencePhotoPreview && (
                    <div className="evidence-upload-preview-box">
                      <div className="upload-preview-header">
                        <FolderLock size={12} className="text-cyan" />
                        <span className="upload-preview-badge-text">UPLOADED EVIDENCE PREVIEW</span>
                      </div>
                      <div className="upload-preview-img-container">
                        <img src={evidencePhotoPreview} alt="Evidence Preview" className="upload-preview-img" />
                      </div>
                      <div className="upload-preview-footer">
                        <span>{selectedFile?.name}</span>
                        <span>{formatBytes(selectedFile?.size)} • {selectedFile?.type}</span>
                      </div>
                    </div>
                  )}

                  {/* Upload Progress Bar */}
                  {uploadingEvidence && (
                    <div className="upload-progress-container">
                      <div className="progress-label-row">
                        <span>STORING & COMPUTING SHA-256 HASH...</span>
                        <span>{uploadProgress}%</span>
                      </div>
                      <div className="progress-track">
                        <div className="progress-fill" style={{ width: `${uploadProgress}%` }}></div>
                      </div>
                    </div>
                  )}

                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">EVIDENCE TITLE <span className="req">*</span></label>
                      <input 
                        type="text" 
                        placeholder="e.g. CCTV Footage Excerpt - North Gate"
                        value={evTitle}
                        onChange={(e) => setEvTitle(e.target.value)}
                        className="case-input"
                        disabled={uploadingEvidence}
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">EVIDENCE CATEGORY <span className="req">*</span></label>
                      <select 
                        value={evType}
                        onChange={(e) => setEvType(e.target.value)}
                        className="case-select"
                        disabled={uploadingEvidence}
                        required
                      >
                        <option value="DOCUMENT">DOCUMENT</option>
                        <option value="IMAGE">IMAGE</option>
                        <option value="VIDEO">VIDEO</option>
                        <option value="AUDIO">AUDIO</option>
                        <option value="FORENSIC_REPORT">FORENSIC REPORT</option>
                        <option value="CCTV">CCTV FOOTAGE</option>
                        <option value="CALL_RECORD">CALL RECORD (CDR)</option>
                        <option value="DIGITAL_DEVICE">DIGITAL DEVICE EXTRACTION</option>
                        <option value="OTHER">OTHER</option>
                      </select>
                    </div>
                  </div>

                  <div className="form-row-grid-3">
                    <div className="form-group">
                      <label className="form-label label-system">COLLECTION DATE/TIME</label>
                      <input 
                        type="datetime-local" 
                        value={evCollectedAt}
                        onChange={(e) => setEvCollectedAt(e.target.value)}
                        className="case-input"
                        disabled={uploadingEvidence}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">COLLECTION LOCATION</label>
                      <input 
                        type="text" 
                        placeholder="e.g. Gandhi Park North Gate"
                        value={evCollectedLocation}
                        onChange={(e) => setEvCollectedLocation(e.target.value)}
                        className="case-input"
                        disabled={uploadingEvidence}
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">COLLECTED BY (OFFICER)</label>
                      <input 
                        type="text" 
                        placeholder="e.g. SI K. Vijay Anand"
                        value={evCollectedBy}
                        onChange={(e) => setEvCollectedBy(e.target.value)}
                        className="case-input"
                        disabled={uploadingEvidence}
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">EVIDENCE DESCRIPTION & SUMMARY</label>
                    <textarea 
                      placeholder="Brief description of the evidence file, circumstances of recovery, or forensic value."
                      value={evDescription}
                      onChange={(e) => setEvDescription(e.target.value)}
                      rows={3}
                      className="case-textarea"
                      disabled={uploadingEvidence}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">CHAIN OF CUSTODY & TECHNICAL NOTES</label>
                    <input 
                      type="text" 
                      placeholder="e.g. Sealed in anti-static tamper bag #402. Digital timestamp verified."
                      value={evNotes}
                      onChange={(e) => setEvNotes(e.target.value)}
                      className="case-input"
                      disabled={uploadingEvidence}
                    />
                  </div>

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsUploadEvidenceModalOpen(false)}
                      disabled={uploadingEvidence}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={uploadingEvidence || !selectedFile}
                    >
                      <UploadCloud size={15} />
                      <span>{uploadingEvidence ? 'UPLOADING & HASHING...' : 'SECURELY UPLOAD EVIDENCE'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL 6: EVIDENCE DETAILS & VERIFICATION AUDIT VIEW */}
          {isDetailModalOpen && selectedEvidence && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent detail-evidence-modal">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <FolderLock size={18} className="modal-title-icon" />
                    <span>EVIDENCE DOSSIER — {selectedEvidence.evidenceNumber}</span>
                  </div>
                  <button className="btn-close-modal" onClick={() => setIsDetailModalOpen(false)}>
                    <X size={18} />
                  </button>
                </div>

                <div className="modal-form">
                  {/* Evidence Top Banner */}
                  <div className="ev-detail-banner">
                    <div className="ev-banner-title-col">
                      <div className="ev-type-row">
                        {getEvidenceTypeIcon(selectedEvidence.evidenceType)}
                        <span className="ev-type-text">{selectedEvidence.evidenceType.replace(/_/g, ' ')}</span>
                      </div>
                      <h2 className="ev-detail-title">{selectedEvidence.title}</h2>
                      <div className="ev-filename">{selectedEvidence.originalFileName}</div>
                    </div>

                    <div className="ev-banner-actions">
                      <span className={`entity-status-badge ${getEvidenceStatusBadgeClass(selectedEvidence.status)}`}>
                        {(selectedEvidence.status || 'UPLOADED').replace(/_/g, ' ')}
                      </span>

                      <button 
                        className="btn-download-prominent"
                        onClick={() => handleDownload(selectedEvidence)}
                      >
                        <Download size={15} />
                        <span>DOWNLOAD FILE ({formatBytes(selectedEvidence.fileSize)})</span>
                      </button>
                    </div>
                  </div>

                  {/* Forensic Metadata Grid */}
                  <div className="fir-details-grid">
                    <div className="fir-meta-item">
                      <span className="fir-meta-label">EVIDENCE NUMBER</span>
                      <span className="fir-meta-val mono-id">{selectedEvidence.evidenceNumber}</span>
                    </div>

                    <div className="fir-meta-item">
                      <span className="fir-meta-label">MIME TYPE</span>
                      <span className="fir-meta-val">{selectedEvidence.mimeType || 'binary/octet-stream'}</span>
                    </div>

                    <div className="fir-meta-item">
                      <span className="fir-meta-label">COLLECTED AT</span>
                      <span className="fir-meta-val">
                        {selectedEvidence.collectedAt ? new Date(selectedEvidence.collectedAt).toLocaleString() : 'N/A'}
                      </span>
                    </div>

                    <div className="fir-meta-item">
                      <span className="fir-meta-label">COLLECTED LOCATION</span>
                      <span className="fir-meta-val">{selectedEvidence.collectedLocation || 'N/A'}</span>
                    </div>

                    <div className="fir-meta-item">
                      <span className="fir-meta-label">COLLECTED BY</span>
                      <span className="fir-meta-val">{selectedEvidence.collectedBy || 'N/A'}</span>
                    </div>

                    <div className="fir-meta-item">
                      <span className="fir-meta-label">CATALOGED BY OFFICER</span>
                      <span className="fir-meta-val">{selectedEvidence.createdByName || 'System Officer'}</span>
                    </div>
                  </div>

                  {/* Cryptographic SHA-256 Hash Display */}
                  <div className="hash-box-panel">
                    <div className="hash-header">
                      <div className="hash-label">
                        <Hash size={14} />
                        <span>CRYPTOGRAPHIC SHA-256 INTEGRITY HASH</span>
                      </div>
                      <button 
                        className="btn-copy-hash"
                        onClick={() => handleCopyHash(selectedEvidence.fileHash)}
                      >
                        <Copy size={13} />
                        <span>{copiedHash ? 'HASH COPIED!' : 'COPY HASH'}</span>
                      </button>
                    </div>
                    <div className="hash-content-val mono-hash-full">
                      {selectedEvidence.fileHash || 'Calculating...'}
                    </div>
                  </div>

                  {/* Description & Chain of Custody */}
                  {selectedEvidence.description && (
                    <div className="fir-narrative-section">
                      <div className="fir-narrative-title">EVIDENCE DESCRIPTION & CONTEXT</div>
                      <div className="fir-narrative-text">{selectedEvidence.description}</div>
                    </div>
                  )}

                  {selectedEvidence.notes && (
                    <div className="fir-narrative-section">
                      <div className="fir-narrative-title">CHAIN OF CUSTODY / FORENSIC NOTES</div>
                      <div className="fir-narrative-text">{selectedEvidence.notes}</div>
                    </div>
                  )}

                  {/* Investigator Status Transition Form */}
                  <div className="status-transition-box cs-panel">
                    <div className="transition-title">INVESTIGATOR VERIFICATION & STATUS UPDATE</div>
                    <form onSubmit={handleUpdateStatusSubmit} className="transition-form">
                      <div className="form-row-grid">
                        <div className="form-group">
                          <label className="form-label label-system">TARGET STATUS</label>
                          <select 
                            value={statusUpdateVal}
                            onChange={(e) => setStatusUpdateVal(e.target.value)}
                            className="case-select"
                            disabled={updatingStatus}
                          >
                            <option value="UPLOADED">UPLOADED (INITIAL)</option>
                            <option value="UNDER_REVIEW">UNDER REVIEW</option>
                            <option value="VERIFIED">VERIFIED (INVESTIGATOR CONFIRMED)</option>
                            <option value="REJECTED">REJECTED (INADMISSIBLE / DEFECTIVE)</option>
                            <option value="ARCHIVED">ARCHIVED</option>
                          </select>
                        </div>

                        <div className="form-group">
                          <label className="form-label label-system">VERIFICATION / REVIEW NOTES</label>
                          <input 
                            type="text" 
                            placeholder="e.g. Verified integrity and matched scene photographs."
                            value={statusNotes}
                            onChange={(e) => setStatusNotes(e.target.value)}
                            className="case-input"
                            disabled={updatingStatus}
                          />
                        </div>
                      </div>

                      <div className="transition-action-row">
                        <button 
                          type="submit" 
                          className="btn-update-status"
                          disabled={updatingStatus || statusUpdateVal === selectedEvidence.status}
                        >
                          <FileCheck size={14} />
                          <span>{updatingStatus ? 'UPDATING STATUS...' : 'APPLY STATUS CHANGE'}</span>
                        </button>
                      </div>
                    </form>
                  </div>

                  {/* Evidence Audit Trail History */}
                  <div className="audit-trail-section">
                    <div className="audit-trail-title">
                      <Layers size={14} />
                      <span>EVIDENCE AUDIT LOG & CHAIN OF CUSTODY HISTORY</span>
                    </div>

                    {evidenceAuditLogs.length === 0 ? (
                      <div className="audit-empty">No audit events recorded yet.</div>
                    ) : (
                      <div className="audit-logs-list">
                        {evidenceAuditLogs.map((log) => (
                          <div key={log.id} className="audit-log-row">
                            <div className="audit-time">
                              {log.createdAt ? new Date(log.createdAt).toLocaleString() : 'Recent'}
                            </div>
                            <div className="audit-action-tag">{log.action}</div>
                            <div className="audit-officer">{log.officerName} ({log.officerBadge})</div>
                            <div className="audit-details">{log.details}</div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsDetailModalOpen(false)}
                    >
                      CLOSE DOSSIER
                    </button>
                    <button 
                      type="button" 
                      className="btn-edit-subtle"
                      onClick={() => {
                        setIsDetailModalOpen(false);
                        handleOpenEditEvidenceModal(selectedEvidence);
                      }}
                    >
                      <Edit3 size={14} />
                      <span>EDIT METADATA</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* MODAL 7: EDIT EVIDENCE METADATA */}
          {isEditEvidenceModalOpen && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <Edit3 size={18} className="modal-title-icon" />
                    <span>EDIT EVIDENCE METADATA</span>
                  </div>
                  <button className="btn-close-modal" onClick={() => setIsEditEvidenceModalOpen(false)}>
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSaveEditEvidenceSubmit} className="modal-form">
                  <div className="form-row-grid">
                    <div className="form-group">
                      <label className="form-label label-system">TITLE <span className="req">*</span></label>
                      <input 
                        type="text" 
                        value={editEvTitle}
                        onChange={(e) => setEditEvTitle(e.target.value)}
                        className="case-input"
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">CATEGORY</label>
                      <select 
                        value={editEvType}
                        onChange={(e) => setEditEvType(e.target.value)}
                        className="case-select"
                      >
                        <option value="DOCUMENT">DOCUMENT</option>
                        <option value="IMAGE">IMAGE</option>
                        <option value="VIDEO">VIDEO</option>
                        <option value="AUDIO">AUDIO</option>
                        <option value="FORENSIC_REPORT">FORENSIC REPORT</option>
                        <option value="CCTV">CCTV FOOTAGE</option>
                        <option value="CALL_RECORD">CALL RECORD (CDR)</option>
                        <option value="DIGITAL_DEVICE">DIGITAL DEVICE EXTRACTION</option>
                        <option value="OTHER">OTHER</option>
                      </select>
                    </div>
                  </div>

                  <div className="form-row-grid-3">
                    <div className="form-group">
                      <label className="form-label label-system">COLLECTION DATE/TIME</label>
                      <input 
                        type="datetime-local" 
                        value={editEvCollectedAt}
                        onChange={(e) => setEditEvCollectedAt(e.target.value)}
                        className="case-input"
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">COLLECTION LOCATION</label>
                      <input 
                        type="text" 
                        value={editEvCollectedLocation}
                        onChange={(e) => setEditEvCollectedLocation(e.target.value)}
                        className="case-input"
                      />
                    </div>

                    <div className="form-group">
                      <label className="form-label label-system">COLLECTED BY</label>
                      <input 
                        type="text" 
                        value={editEvCollectedBy}
                        onChange={(e) => setEditEvCollectedBy(e.target.value)}
                        className="case-input"
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">DESCRIPTION</label>
                    <textarea 
                      value={editEvDescription}
                      onChange={(e) => setEditEvDescription(e.target.value)}
                      rows={3}
                      className="case-textarea"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">CHAIN OF CUSTODY & TECHNICAL NOTES</label>
                    <input 
                      type="text" 
                      value={editEvNotes}
                      onChange={(e) => setEditEvNotes(e.target.value)}
                      className="case-input"
                    />
                  </div>

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsEditEvidenceModalOpen(false)}
                      disabled={savingEvidence}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={savingEvidence}
                    >
                      <Save size={15} />
                      <span>{savingEvidence ? 'SAVING...' : 'SAVE CHANGES'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL: TRIGGER TEST CASE NOTIFICATION */}
          {isTestAlertModalOpen && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent" style={{ maxWidth: '520px' }}>
                <div className="modal-header">
                  <div className="modal-title-row">
                    <Send size={18} className="modal-title-icon" />
                    <span>TRIGGER DEVELOPMENT TEST ALERT</span>
                  </div>
                  <button 
                    className="btn-close-modal" 
                    onClick={() => {
                      setIsTestAlertModalOpen(false);
                      setTestAlertSuccess('');
                      setTestAlertError('');
                    }}
                  >
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSendTestAlert} className="modal-form">
                  <p className="test-modal-desc">
                    Dispatches a simulated high-priority case alert to the primary station investigator on record. Non-blocking delivery ensures case integrity is preserved even if external messaging providers are offline.
                  </p>

                  <div className="form-group">
                    <label className="form-label label-system">CASE RECORD</label>
                    <input 
                      type="text" 
                      value={`#${caseData?.caseNumber || id} — ${caseData?.title || 'Case'}`}
                      disabled
                      className="case-input notif-input-disabled"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">DISPATCH CHANNEL</label>
                    <select
                      value={testAlertChannel}
                      onChange={(e) => setTestAlertChannel(e.target.value)}
                      className="case-select"
                    >
                      <option value="EMAIL">EMAIL (Spring Mail SMTP)</option>
                      <option value="WHATSAPP">WHATSAPP (Meta Cloud API)</option>
                    </select>
                  </div>

                  <div className="notif-policy-note">
                    <div className="policy-note-title">
                      <ShieldAlert size={13} className="text-red" />
                      <span>Station Authorization & Fallback Policy:</span>
                    </div>
                    <p>
                      Recipient address is resolved securely from the authenticated officer profile on the server. If SMTP or WhatsApp Cloud tokens are not configured in <code className="policy-code">application.properties</code>, the system safely records the dispatch in development fallback mode.
                    </p>
                  </div>

                  {testAlertSuccess && (
                    <div className="notif-modal-feedback success">
                      <CheckCircle size={14} />
                      <span>{testAlertSuccess}</span>
                    </div>
                  )}

                  {testAlertError && (
                    <div className="notif-modal-feedback error">
                      <AlertTriangle size={14} />
                      <span>{testAlertError}</span>
                    </div>
                  )}

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => {
                        setIsTestAlertModalOpen(false);
                        setTestAlertSuccess('');
                        setTestAlertError('');
                      }}
                      disabled={sendingTestAlert}
                    >
                      CLOSE
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={sendingTestAlert}
                    >
                      <Send size={14} />
                      <span>{sendingTestAlert ? 'DISPATCHING...' : 'DISPATCH ALERT'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL: REVIEW & VERIFY PUBLIC INFORMATION */}
          {isReviewPublicModalOpen && reviewingPublicItem && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent" style={{ maxWidth: '560px' }}>
                <div className="modal-header">
                  <div className="modal-title-row">
                    <ShieldAlert size={18} className="modal-title-icon" />
                    <span>INVESTIGATOR VERIFICATION REVIEW</span>
                  </div>
                  <button
                    className="btn-close-modal"
                    onClick={() => {
                      setIsReviewPublicModalOpen(false);
                      setReviewingPublicItem(null);
                      setPublicReviewError('');
                    }}
                  >
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSavePublicReview} className="modal-form">
                  <div className="form-group">
                    <label className="form-label label-system">ARTICLE / SOURCE RECORD</label>
                    <input
                      type="text"
                      value={reviewingPublicItem.title}
                      disabled
                      className="case-input notif-input-disabled"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">ORIGINAL PUBLISHER URL</label>
                    <input
                      type="text"
                      value={reviewingPublicItem.sourceUrl}
                      disabled
                      className="case-input notif-input-disabled font-mono text-xs"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">
                      VERIFICATION STATUS <span className="req">*</span>
                    </label>
                    <select
                      value={reviewVerificationStatus}
                      onChange={(e) => setReviewVerificationStatus(e.target.value)}
                      className="case-select"
                      required
                    >
                      <option value="UNVERIFIED">UNVERIFIED — Pending initial officer review</option>
                      <option value="UNDER_REVIEW">UNDER_REVIEW — Under active corroboration</option>
                      <option value="CORROBORATED">CORROBORATED — Supported by official case records (Notes Required)</option>
                      <option value="CONTRADICTED">CONTRADICTED — In conflict with confirmed case facts (Notes Required)</option>
                      <option value="NOT_RELEVANT">NOT_RELEVANT — Irrelevant / unrelated to investigation (Notes Required)</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">
                      INVESTIGATOR VERIFICATION NOTES
                      {(reviewVerificationStatus === 'CORROBORATED' ||
                        reviewVerificationStatus === 'CONTRADICTED' ||
                        reviewVerificationStatus === 'NOT_RELEVANT') && (
                        <span className="req"> * (MANDATORY)</span>
                      )}
                    </label>
                    <textarea
                      value={reviewNotes}
                      onChange={(e) => setReviewNotes(e.target.value)}
                      placeholder="Explain findings, official evidence cross-references, discrepancies, or reason for status..."
                      className="case-textarea"
                      rows={4}
                      required={
                        reviewVerificationStatus === 'CORROBORATED' ||
                        reviewVerificationStatus === 'CONTRADICTED' ||
                        reviewVerificationStatus === 'NOT_RELEVANT'
                      }
                    />
                  </div>

                  <div className="notif-policy-note">
                    <div className="policy-note-title">
                      <ShieldAlert size={13} className="text-red" />
                      <span>Evidence Separation Notice:</span>
                    </div>
                    <p>
                      Marking this record as <strong>CORROBORATED</strong> records investigator validation in the audit trail, but does <strong>NOT</strong> convert this web article into forensic evidence. Evidence must be uploaded through the secure Evidence Vault.
                    </p>
                  </div>

                  {publicReviewError && (
                    <div className="notif-modal-feedback error">
                      <AlertTriangle size={14} />
                      <span>{publicReviewError}</span>
                    </div>
                  )}

                  <div className="modal-actions">
                    <button
                      type="button"
                      className="btn-cancel"
                      onClick={() => {
                        setIsReviewPublicModalOpen(false);
                        setReviewingPublicItem(null);
                        setPublicReviewError('');
                      }}
                      disabled={savingPublicReview}
                    >
                      CANCEL
                    </button>
                    <button
                      type="submit"
                      className="btn-save-edit"
                      disabled={savingPublicReview}
                    >
                      <Save size={14} />
                      <span>{savingPublicReview ? 'SAVING...' : 'SAVE VERIFICATION'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL: MOCK WHATSAPP PREVIEW */}
          {isWaModalOpen && (
            <WhatsAppPreviewModal
              notification={selectedWaNotif}
              onClose={() => {
                setIsWaModalOpen(false);
                setSelectedWaNotif(null);
              }}
            />
          )}

          {/* MODAL: UPLOAD SUSPECT PHOTOGRAPH */}
          {isSuspectPhotoModalOpen && selectedSuspectForPhoto && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent suspect-photo-modal">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <Camera size={18} className="modal-title-icon text-amber" />
                    <span>RECORD CASE PHOTOGRAPH — {selectedSuspectForPhoto.fullName}</span>
                  </div>
                  <button 
                    className="btn-close-modal" 
                    onClick={() => !uploadingSuspectPhoto && setIsSuspectPhotoModalOpen(false)}
                    disabled={uploadingSuspectPhoto}
                  >
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSaveSuspectPhoto} className="modal-form">
                  <div className="suspect-photo-modal-header-info">
                    <div className="suspect-modal-meta-row">
                      <span className="meta-lbl">SUBJECT:</span>
                      <span className="meta-val">{selectedSuspectForPhoto.fullName}</span>
                      {selectedSuspectForPhoto.alias && (
                        <span className="meta-sub">({selectedSuspectForPhoto.alias})</span>
                      )}
                    </div>
                    <span className={`entity-status-badge ${getSuspectStatusClass(selectedSuspectForPhoto.investigationStatus)}`}>
                      {(selectedSuspectForPhoto.investigationStatus || 'UNDER_INVESTIGATION').replace(/_/g, ' ')}
                    </span>
                  </div>

                  {/* Mandatory Law Enforcement Disclaimer */}
                  <div className="suspect-photo-disclaimer-card">
                    <ShieldAlert size={15} className="text-amber" />
                    <span>
                      <strong>CASE PHOTO RECORD:</strong> Photographic documentation is attached to the case dossier for investigatory record-keeping. The system does not conduct biometric identification or facial-recognition inferences.
                    </span>
                  </div>

                  {/* File Selector */}
                  <div className="file-drop-zone">
                    <input 
                      type="file" 
                      id="suspect-photo-input"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={handleSuspectPhotoSelection}
                      disabled={uploadingSuspectPhoto}
                      className="file-hidden-input"
                    />
                    <label htmlFor="suspect-photo-input" className="file-drop-label">
                      <Camera size={32} className="drop-icon text-amber" />
                      <div className="drop-title">
                        {suspectPhotoFile ? suspectPhotoFile.name : 'SELECT SUSPECT / PERSON OF INTEREST PHOTO'}
                      </div>
                      <div className="drop-subtitle">
                        Allowed: JPG, JPEG, PNG, WEBP (Max: 25 MB)
                      </div>
                    </label>
                  </div>

                  {/* Photo Preview */}
                  {suspectPhotoPreview && (
                    <div className="suspect-modal-preview-box">
                      <div className="suspect-preview-badge-row">
                        <Camera size={12} className="text-amber" />
                        <span className="suspect-preview-badge-text">CASE PHOTO PREVIEW</span>
                      </div>
                      <div className="suspect-preview-img-container">
                        <img 
                          src={suspectPhotoPreview} 
                          alt="Suspect Preview" 
                          className="suspect-preview-img" 
                        />
                      </div>
                    </div>
                  )}

                  {suspectPhotoError && (
                    <div className="notif-modal-feedback error">
                      <AlertTriangle size={15} />
                      <span>{suspectPhotoError}</span>
                    </div>
                  )}

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsSuspectPhotoModalOpen(false)}
                      disabled={uploadingSuspectPhoto}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit btn-save-amber"
                      disabled={uploadingSuspectPhoto || !suspectPhotoFile}
                    >
                      <Save size={15} />
                      <span>{uploadingSuspectPhoto ? 'STORING PHOTO...' : 'SAVE CASE PHOTOGRAPH'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL: UPLOAD VICTIM PHOTOGRAPH */}
          {isVictimPhotoModalOpen && selectedVictimForPhoto && (
            <div className="modal-backdrop">
              <div className="edit-modal cs-panel cs-panel-accent victim-photo-modal">
                <div className="modal-header">
                  <div className="modal-title-row">
                    <Camera size={18} className="modal-title-icon text-cyan" />
                    <span>RECORD VICTIM CASE PHOTOGRAPH — {selectedVictimForPhoto.fullName}</span>
                  </div>
                  <button 
                    className="btn-close-modal" 
                    onClick={() => !uploadingVictimPhoto && setIsVictimPhotoModalOpen(false)}
                    disabled={uploadingVictimPhoto}
                  >
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleSaveVictimPhoto} className="modal-form">
                  <div className="suspect-photo-modal-header-info">
                    <div className="suspect-modal-meta-row">
                      <span className="meta-lbl">SUBJECT:</span>
                      <span className="meta-val">{selectedVictimForPhoto.fullName}</span>
                      {selectedVictimForPhoto.relationshipToCase && (
                        <span className="meta-sub">({selectedVictimForPhoto.relationshipToCase})</span>
                      )}
                    </div>
                    <span className={`entity-status-badge ${getVictimStatusClass(selectedVictimForPhoto.status)}`}>
                      {selectedVictimForPhoto.status || 'ACTIVE'}
                    </span>
                  </div>

                  {/* Mandatory Law Enforcement Disclaimer */}
                  <div className="suspect-photo-disclaimer-card">
                    <ShieldAlert size={15} className="text-cyan" />
                    <span>
                      <strong>VICTIM CASE PHOTO RECORD:</strong> Attached to the official victim dossier for law enforcement record-keeping. The system does not conduct biometric identification or facial-recognition inferences.
                    </span>
                  </div>

                  {/* File Selector */}
                  <div className="file-drop-zone">
                    <input 
                      type="file" 
                      id="victim-photo-input"
                      accept="image/jpeg,image/png,image/webp"
                      onChange={handleVictimPhotoSelection}
                      disabled={uploadingVictimPhoto}
                      className="file-hidden-input"
                    />
                    <label htmlFor="victim-photo-input" className="file-drop-label">
                      <Camera size={32} className="drop-icon text-cyan" />
                      <div className="drop-title">
                        {victimPhotoFile ? victimPhotoFile.name : 'SELECT OFFICIAL VICTIM PHOTOGRAPH'}
                      </div>
                      <div className="drop-subtitle">
                        Allowed: JPG, JPEG, PNG, WEBP (Max: 25 MB)
                      </div>
                    </label>
                  </div>

                  {/* Photo Preview */}
                  {victimPhotoPreview && (
                    <div className="suspect-modal-preview-box">
                      <div className="suspect-preview-badge-row">
                        <Camera size={12} className="text-cyan" />
                        <span className="suspect-preview-badge-text">CASE PHOTO PREVIEW</span>
                      </div>
                      <div className="suspect-preview-img-container">
                        <img 
                          src={victimPhotoPreview} 
                          alt="Victim Preview" 
                          className="suspect-preview-img" 
                        />
                      </div>
                    </div>
                  )}

                  {victimPhotoError && (
                    <div className="notif-modal-feedback error">
                      <AlertTriangle size={15} />
                      <span>{victimPhotoError}</span>
                    </div>
                  )}

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsVictimPhotoModalOpen(false)}
                      disabled={uploadingVictimPhoto}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-save-edit"
                      disabled={uploadingVictimPhoto || !victimPhotoFile}
                    >
                      <Save size={15} />
                      <span>{uploadingVictimPhoto ? 'STORING PHOTO...' : 'SAVE CASE PHOTOGRAPH'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL: GENERATE AI ILLUSTRATION FOR EVIDENCE ITEM */}
          {isEvidenceAiModalOpen && selectedEvidenceForAi && (
            <div className="modal-backdrop" onClick={() => !generatingEvidenceAi && setIsEvidenceAiModalOpen(false)}>
              <div className="edit-modal cs-panel cs-panel-accent evidence-ai-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                  <div className="modal-title-row">
                    <Sparkles size={18} className="modal-title-icon text-purple" />
                    <span>GENERATE AI SCENE ILLUSTRATION</span>
                  </div>
                  <button 
                    className="btn-close-modal" 
                    onClick={() => !generatingEvidenceAi && setIsEvidenceAiModalOpen(false)}
                    disabled={generatingEvidenceAi}
                  >
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleGenerateEvidenceIllustration} className="modal-form">
                  <div className="evidence-ai-target-info">
                    <div className="target-info-item">
                      <span className="lbl">EVIDENCE RECORD:</span>
                      <span className="val">{selectedEvidenceForAi.title}</span>
                      <span className="mono-num">({selectedEvidenceForAi.evidenceNumber})</span>
                    </div>
                  </div>

                  {/* MANDATORY WARNING */}
                  <div className="ai-evidentiary-warning-box mt-2 mb-2">
                    <div className="warning-box-header">
                      <ShieldAlert size={14} className="text-crimson" />
                      <span className="warning-box-title">AI-GENERATED ILLUSTRATION — NOT REAL EVIDENCE</span>
                    </div>
                    <p className="warning-box-body">
                      This generates a synthetic visualization attached to this record. The item will be marked with status <strong>AI_ILLUSTRATION</strong> and cannot be verified as authentic forensic evidence.
                    </p>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">
                      SCENE DESCRIPTION / PROMPT <span className="req">*</span>
                    </label>
                    <textarea
                      value={evidenceAiPrompt}
                      onChange={(e) => setEvidenceAiPrompt(e.target.value)}
                      placeholder="e.g. Detailed crime scene perspective showing spatial layout, lighting angles, and point of entry..."
                      rows={4}
                      className="case-textarea prompt-textarea"
                      disabled={generatingEvidenceAi}
                      required
                    />
                  </div>

                  {/* Sample prompt chips */}
                  <div className="prompt-suggestions-container mb-2">
                    <span className="suggestions-label">SUGGESTIONS:</span>
                    <div className="suggestions-chips-grid">
                      {[
                        `Realistic reconstruction of ${selectedEvidenceForAi.title} in ambient low-light night conditions`,
                        `Spatial blueprint perspective of scene showing placement of ${selectedEvidenceForAi.title}`,
                        `Close-up investigative illustration of ${selectedEvidenceForAi.title} with measurement reference markers`
                      ].map((chip, idx) => (
                        <button
                          key={idx}
                          type="button"
                          className="prompt-preset-chip"
                          disabled={generatingEvidenceAi}
                          onClick={() => setEvidenceAiPrompt(chip)}
                        >
                          <Wand2 size={11} />
                          <span className="preset-text">{chip}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  {evidenceAiError && (
                    <div className="notif-modal-feedback error mt-2">
                      <AlertTriangle size={15} />
                      <span>{evidenceAiError}</span>
                    </div>
                  )}

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsEvidenceAiModalOpen(false)}
                      disabled={generatingEvidenceAi}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-generate-ai-illustration"
                      disabled={generatingEvidenceAi || !evidenceAiPrompt.trim()}
                    >
                      {generatingEvidenceAi ? (
                        <>
                          <RefreshCw size={14} className="spin-icon" />
                          <span>SYNTHESIZING ILLUSTRATION...</span>
                        </>
                      ) : (
                        <>
                          <Sparkles size={14} />
                          <span>GENERATE ILLUSTRATION</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL: UPLOAD AUTHENTIC EVIDENCE FILE (REPLACES DEFAULT AI ILLUSTRATION) */}
          {isReplaceModalOpen && selectedEvidenceForReplace && (
            <div className="modal-backdrop" onClick={() => !replacingFile && setIsReplaceModalOpen(false)}>
              <div className="edit-modal cs-panel cs-panel-accent replace-evidence-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                  <div className="modal-title-row">
                    <UploadCloud size={18} className="modal-title-icon text-cyan" />
                    <span>UPLOAD AUTHENTIC EVIDENCE FILE</span>
                  </div>
                  <button 
                    className="btn-close-modal" 
                    onClick={() => !replacingFile && setIsReplaceModalOpen(false)}
                    disabled={replacingFile}
                  >
                    <X size={18} />
                  </button>
                </div>

                <form onSubmit={handleReplaceEvidenceSubmit} className="modal-form">
                  <div className="evidence-ai-target-info">
                    <div className="target-info-item">
                      <span className="lbl">EVIDENCE ITEM:</span>
                      <span className="val">{selectedEvidenceForReplace.title}</span>
                      <span className="mono-num">({selectedEvidenceForReplace.evidenceNumber})</span>
                    </div>
                    <div className="target-info-item mt-1">
                      <span className="lbl">TYPE:</span>
                      <span className="val">{selectedEvidenceForReplace.evidenceType}</span>
                    </div>
                  </div>

                  <div className="replace-notice-box mt-2 mb-2">
                    <div className="notice-box-header">
                      <FolderLock size={14} className="text-cyan" />
                      <span className="notice-box-title">CHAIN-OF-CUSTODY EVIDENCE REPLACEMENT</span>
                    </div>
                    <p className="notice-box-body">
                      Uploading an authentic evidence file will replace the current default illustration, recompute cryptographic SHA-256 integrity hash, and promote status to <strong>UPLOADED EVIDENCE</strong>.
                    </p>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">
                      SELECT REAL EVIDENCE FILE (JPG, PNG, WEBP, PDF, MP4, MP3, etc.) <span className="req">*</span>
                    </label>
                    <input
                      type="file"
                      id="replace-evidence-file-input"
                      onChange={handleReplaceFileSelection}
                      className="case-input file-input"
                      disabled={replacingFile}
                      required
                    />
                  </div>

                  {replacePreview && (
                    <div className="preview-container mt-2 mb-2">
                      <div className="preview-label">IMAGE PREVIEW:</div>
                      <div className="preview-box">
                        <img src={replacePreview} alt="Evidence preview" className="preview-img-evidence" />
                      </div>
                    </div>
                  )}

                  {replaceError && (
                    <div className="notif-modal-feedback error mt-2">
                      <AlertTriangle size={15} />
                      <span>{replaceError}</span>
                    </div>
                  )}

                  <div className="modal-actions">
                    <button 
                      type="button" 
                      className="btn-cancel"
                      onClick={() => setIsReplaceModalOpen(false)}
                      disabled={replacingFile}
                    >
                      CANCEL
                    </button>
                    <button 
                      type="submit" 
                      className="btn-submit-upload-evidence"
                      disabled={replacingFile || !replaceFile}
                    >
                      {replacingFile ? (
                        <>
                          <RefreshCw size={14} className="spin-icon" />
                          <span>UPLOADING & HASHING FILE...</span>
                        </>
                      ) : (
                        <>
                          <UploadCloud size={14} />
                          <span>UPLOAD REAL EVIDENCE</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* MODAL: IMAGE LIGHTBOX & FORENSIC VIEWER */}
          {activeLightboxImage && (
            <div className="modal-backdrop lightbox-backdrop" onClick={() => setActiveLightboxImage(null)}>
              <div className="lightbox-modal cs-panel" onClick={(e) => e.stopPropagation()}>
                <div className="lightbox-header">
                  <div className="lightbox-title-group">
                    {activeLightboxImage.badgeType === 'ai' ? (
                      <div className="badge-ai-lightbox">
                        <ShieldAlert size={12} />
                        <span>{activeLightboxImage.badge}</span>
                      </div>
                    ) : activeLightboxImage.badgeType === 'photo' ? (
                      <div className="badge-photo-lightbox">
                        <Camera size={12} />
                        <span>{activeLightboxImage.badge}</span>
                      </div>
                    ) : (
                      <div className="badge-evidence-lightbox">
                        <FolderLock size={12} />
                        <span>{activeLightboxImage.badge}</span>
                      </div>
                    )}
                    <h3 className="lightbox-title">{activeLightboxImage.title}</h3>
                  </div>

                  <div className="lightbox-header-actions">
                    {activeLightboxImage.url && (
                      <a
                        href={activeLightboxImage.url}
                        target="_blank"
                        rel="noreferrer"
                        className="btn-lightbox-action"
                        title="Open full resolution in new tab"
                      >
                        <ExternalLink size={16} />
                      </a>
                    )}
                    <button 
                      className="btn-lightbox-close" 
                      onClick={() => setActiveLightboxImage(null)}
                      title="Close viewer"
                    >
                      <X size={18} />
                    </button>
                  </div>
                </div>

                <div className="lightbox-image-viewport">
                  {activeLightboxImage.url ? (
                    <img 
                      src={activeLightboxImage.url} 
                      alt={activeLightboxImage.title} 
                      className="lightbox-full-img"
                      onError={(e) => {
                        e.target.style.display = 'none';
                        if (e.target.nextSibling) {
                          e.target.nextSibling.style.display = 'flex';
                        }
                      }}
                    />
                  ) : null}
                  {activeLightboxImage.svgContent ? (
                    <div 
                      className="lightbox-svg-container"
                      style={{ 
                        display: activeLightboxImage.url ? 'none' : 'flex', 
                        width: '100%', 
                        maxWidth: '850px', 
                        height: '100%', 
                        maxHeight: '520px', 
                        alignItems: 'center', 
                        justifyContent: 'center' 
                      }}
                      dangerouslySetInnerHTML={{ __html: activeLightboxImage.svgContent }}
                    />
                  ) : null}
                </div>

                {activeLightboxImage.subtitle && (
                  <div className="lightbox-footer">
                    <p className="lightbox-subtitle-text">{activeLightboxImage.subtitle}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* MODAL: INTER-STATION COORDINATION MODAL */}
          {isCoordinationModalOpen && (
            <InterStationCoordinationModal
              isOpen={isCoordinationModalOpen}
              onClose={() => {
                setIsCoordinationModalOpen(false);
                setSelectedCoordinationVictim(null);
              }}
              caseData={caseData}
              victim={selectedCoordinationVictim}
              onSuccess={(reqResult) => {
                showFlash(`Inter-station coordination request successfully dispatched to ${reqResult.destinationStationName}`);
              }}
            />
          )}

        </main>
      </div>

      <style>{`
        .cs-dashboard-layout {
          min-height: 100vh;
          display: flex;
          flex-direction: column;
          background-color: var(--bg-primary);
        }

        .cs-main-wrapper {
          display: flex;
          flex: 1;
          width: 100%;
          min-width: 0;
        }

        @media (max-width: 768px) {
          .cs-main-wrapper {
            flex-direction: column;
          }
        }

        .cs-content-area {
          flex: 1;
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 20px;
          max-width: 1440px;
          margin: 0 auto;
          width: 100%;
          min-width: 0;
        }

        @media (max-width: 768px) {
          .cs-content-area {
            padding: 12px;
            gap: 16px;
          }
        }

        .detail-top-nav {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .btn-back {
          display: flex;
          align-items: center;
          gap: 6px;
          background: transparent;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 6px 12px;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-back:hover {
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        .top-actions {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .btn-generate-pdf {
          display: flex;
          align-items: center;
          gap: 6px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 6px 14px;
          color: white;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
          box-shadow: var(--red-glow-subtle);
        }

        .btn-generate-pdf:hover:not(:disabled) {
          box-shadow: var(--red-glow-medium);
          transform: translateY(-1px);
        }

        .btn-generate-pdf:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .btn-edit-case {
          display: flex;
          align-items: center;
          gap: 6px;
          background: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 6px 14px;
          color: var(--text-primary);
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-edit-case:hover {
          border-color: var(--bright-crime-red);
          box-shadow: var(--red-glow-subtle);
          color: var(--text-bright);
        }

        .flash-success-banner {
          display: flex;
          align-items: center;
          gap: 8px;
          background-color: rgba(16, 185, 129, 0.15);
          border: 1px solid var(--color-success);
          border-radius: 4px;
          padding: 10px 14px;
          color: #10b981;
          font-family: var(--font-subheading);
          font-size: 0.78rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          animation: fadeIn 0.2s ease-in-out;
        }

        /* Dossier Header Panel */
        .dossier-header-panel {
          padding: 20px;
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .dossier-id-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .dossier-case-num {
          display: flex;
          align-items: center;
          gap: 8px;
          font-family: var(--font-mono);
          font-size: 1.1rem;
          font-weight: 700;
          color: var(--bright-crime-red);
          letter-spacing: 0.05em;
        }

        .header-lock-icon { color: var(--bright-crime-red); }

        .dossier-badge-group {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .dossier-title {
          font-size: 1.4rem;
          font-weight: 700;
          margin: 0;
          color: var(--text-bright);
          letter-spacing: 0.04em;
        }

        .quick-controller-bar {
          display: flex;
          align-items: center;
          gap: 24px;
          padding-top: 10px;
          border-top: 1px solid rgba(42, 42, 45, 0.6);
          margin-top: 4px;
        }

        .ctrl-group {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .ctrl-label {
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-muted);
        }

        .quick-select {
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          color: var(--text-primary);
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          padding: 4px 8px;
          outline: none;
          cursor: pointer;
        }

        .quick-select:focus {
          border-color: var(--bright-crime-red);
        }

        /* Dossier Interactive Tabs Bar */
        .dossier-tabs-bar {
          display: flex;
          align-items: center;
          gap: 8px;
          border-bottom: 1px solid var(--border-color);
          padding-bottom: 2px;
          overflow-x: auto;
        }

        .tab-btn {
          display: flex;
          align-items: center;
          gap: 8px;
          background: transparent;
          border: none;
          border-bottom: 2px solid transparent;
          padding: 10px 16px;
          color: var(--text-muted);
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          cursor: pointer;
          transition: all var(--transition-fast);
          white-space: nowrap;
        }

        .tab-btn:hover {
          color: var(--text-primary);
        }

        .tab-btn.active {
          color: var(--bright-crime-red);
          border-bottom-color: var(--bright-crime-red);
          text-shadow: var(--red-glow-subtle);
        }

        /* Overview Grid */
        .dossier-grid {
          display: grid;
          grid-template-columns: 1fr 340px;
          gap: 20px;
        }

        .grid-left-column {
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .metadata-card {
          padding: 16px 20px;
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 16px;
        }

        .meta-item {
          display: flex;
          align-items: flex-start;
          gap: 10px;
        }

        .meta-icon {
          color: var(--bright-crime-red);
          margin-top: 2px;
        }

        .meta-label {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
        }

        .meta-val {
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--text-primary);
          margin-top: 1px;
        }

        .narrative-content {
          padding: 8px 4px;
        }

        .narrative-text {
          font-size: 0.88rem;
          line-height: 1.6;
          color: var(--text-secondary);
          white-space: pre-wrap;
        }

        .narrative-placeholder {
          font-size: 0.82rem;
          color: var(--text-muted);
          font-style: italic;
        }

        .quick-summary-panel {
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .summary-title {
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
          padding-bottom: 8px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.6);
        }

        .summary-items {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .summary-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 0.8rem;
          color: var(--text-secondary);
        }

        .text-green { color: #10b981; }
        .text-amber { color: #f59e0b; }
        .text-red { color: #ff4d4d; }

        .mono-date {
          font-family: var(--font-mono);
          font-size: 0.72rem;
          color: var(--text-muted);
        }

        /* FIR Styles */
        .fir-tab-container, .sub-entity-tab-container, .modules-tab-container {
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .fir-dossier-card {
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .fir-top-bar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding-bottom: 14px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.8);
        }

        .fir-number-badge {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .fir-badge-icon { color: var(--bright-crime-red); }

        .fir-label {
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
        }

        .fir-num {
          font-family: var(--font-mono);
          font-size: 1.25rem;
          font-weight: 700;
          color: var(--text-bright);
        }

        .btn-edit-fir {
          display: flex;
          align-items: center;
          gap: 6px;
          background: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 6px 14px;
          color: var(--text-primary);
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          cursor: pointer;
        }

        .btn-edit-fir:hover {
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        .fir-details-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
          gap: 16px;
          background-color: var(--bg-primary);
          padding: 16px;
          border: 1px solid var(--border-color);
          border-radius: 4px;
        }

        .fir-meta-item {
          display: flex;
          flex-direction: column;
          gap: 3px;
        }

        .fir-meta-label {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
        }

        .fir-meta-val {
          font-size: 0.82rem;
          font-weight: 600;
          color: var(--text-primary);
        }

        .mono-sections {
          font-family: var(--font-mono);
          color: #f59e0b;
        }

        .fir-narrative-section {
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .fir-narrative-title {
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
        }

        .fir-narrative-text {
          font-size: 0.85rem;
          line-height: 1.6;
          color: var(--text-secondary);
          white-space: pre-wrap;
        }

        .fir-footer-meta {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-family: var(--font-mono);
          font-size: 0.7rem;
          color: var(--text-muted);
          padding-top: 10px;
          border-top: 1px solid rgba(42, 42, 45, 0.5);
        }

        .fir-empty-card, .entity-empty-card {
          padding: 50px 20px;
          background-color: var(--bg-panel);
          border: 1px dashed var(--border-color);
          border-radius: 4px;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 14px;
          text-align: center;
        }

        .empty-fir-icon, .empty-mod-icon {
          color: var(--text-muted);
        }

        .empty-fir-title, .empty-title {
          font-family: var(--font-subheading);
          font-size: 0.95rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .empty-fir-desc, .empty-desc {
          font-size: 0.8rem;
          color: var(--text-muted);
          max-width: 440px;
        }

        .btn-register-fir, .btn-add-entity {
          display: flex;
          align-items: center;
          gap: 8px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 8px 16px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
          box-shadow: var(--red-glow-subtle);
        }

        .btn-register-fir:hover, .btn-add-entity:hover {
          box-shadow: var(--red-glow-medium);
          transform: translateY(-1px);
        }

        /* Sub-entities Header & Cards */
        .tab-actions-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .section-title {
          font-size: 1.1rem;
          font-family: var(--font-subheading);
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
          margin: 0;
        }

        .section-subtitle {
          font-size: 0.75rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .legal-disclaimer-card {
          display: flex;
          align-items: center;
          gap: 10px;
          background-color: rgba(234, 88, 12, 0.12);
          border: 1px solid #ea580c;
          border-radius: 4px;
          padding: 10px 14px;
          font-size: 0.75rem;
          color: #fb923c;
          line-height: 1.4;
        }

        .legal-ic {
          flex-shrink: 0;
          color: #fb923c;
        }

        /* Evidence Type Filter Bar */
        .evidence-filter-bar {
          display: flex;
          align-items: center;
          gap: 8px;
          overflow-x: auto;
          padding-bottom: 4px;
        }

        .filter-pill {
          background: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 3px;
          padding: 5px 10px;
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          color: var(--text-muted);
          cursor: pointer;
          transition: all var(--transition-fast);
          white-space: nowrap;
        }

        .filter-pill:hover {
          border-color: var(--text-secondary);
          color: var(--text-primary);
        }

        .filter-pill.active {
          background: rgba(193, 18, 31, 0.15);
          border-color: var(--bright-crime-red);
          color: var(--bright-crime-red);
          box-shadow: var(--red-glow-subtle);
        }

        .entity-cards-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
          gap: 16px;
        }

        .entity-card {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .entity-card-header {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          padding-bottom: 8px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.6);
        }

        .ev-header-info {
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .ev-type-row {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .ev-type-text {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-muted);
        }

        .ev-icon {
          color: var(--bright-crime-red);
        }

        .entity-name {
          font-family: var(--font-subheading);
          font-size: 0.95rem;
          font-weight: 700;
          color: var(--text-bright);
        }

        .entity-sub {
          font-family: var(--font-mono);
          font-size: 0.7rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .mono-id {
          font-family: var(--font-mono);
          color: var(--bright-crime-red);
          font-weight: 700;
        }

        .entity-status-badge {
          display: inline-block;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          padding: 3px 8px;
          border-radius: 3px;
          white-space: nowrap;
        }

        /* Victim & Suspect Badges */
        .vbadge-deceased { background-color: rgba(193, 18, 31, 0.25); border: 1px solid var(--bright-crime-red); color: #ff4d4d; }
        .vbadge-active { background-color: rgba(40, 167, 69, 0.15); border: 1px solid #28a745; color: #85e89d; }
        .vbadge-missing { background-color: rgba(234, 88, 12, 0.15); border: 1px solid #ea580c; color: #fb923c; }
        .vbadge-identified { background-color: rgba(0, 180, 216, 0.15); border: 1px solid #00b4d8; color: #48cae4; }

        .sbadge-investigating { background-color: rgba(193, 18, 31, 0.25); border: 1px solid var(--bright-crime-red); color: #ff4d4d; }
        .sbadge-poi { background-color: rgba(255, 170, 0, 0.15); border: 1px solid #ffaa00; color: #ffaa00; }
        .sbadge-cleared { background-color: rgba(71, 85, 105, 0.2); border: 1px solid #475569; color: #94a3b8; }
        .sbadge-referred { background-color: rgba(147, 51, 234, 0.15); border: 1px solid #9333ea; color: #c084fc; }

        /* Evidence Badges */
        .evbadge-uploaded { background-color: rgba(0, 180, 216, 0.15); border: 1px solid #00b4d8; color: #48cae4; }
        .evbadge-review { background-color: rgba(255, 170, 0, 0.15); border: 1px solid #ffaa00; color: #ffaa00; }
        .evbadge-verified { background-color: rgba(16, 185, 129, 0.2); border: 1px solid #10b981; color: #10b981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.25); }
        .evbadge-rejected { background-color: rgba(193, 18, 31, 0.25); border: 1px solid var(--bright-crime-red); color: #ff4d4d; }
        .evbadge-archived { background-color: rgba(71, 85, 105, 0.2); border: 1px solid #475569; color: #94a3b8; }

        .entity-body {
          display: flex;
          flex-direction: column;
          gap: 6px;
          font-size: 0.8rem;
        }

        .entity-field {
          display: flex;
          gap: 6px;
        }

        .field-lbl {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          color: var(--text-muted);
          min-width: 100px;
        }

        .field-val {
          color: var(--text-primary);
          font-weight: 500;
        }

        .truncate-file {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 200px;
        }

        .mono-hash {
          font-family: var(--font-mono);
          font-size: 0.72rem;
          color: #a7a7aa;
        }

        .entity-desc-box {
          margin-top: 4px;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 3px;
          padding: 8px;
        }

        .field-desc {
          margin: 4px 0 0 0;
          font-size: 0.75rem;
          color: var(--text-secondary);
          line-height: 1.4;
        }

        .entity-card-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding-top: 8px;
          border-top: 1px solid rgba(42, 42, 45, 0.5);
        }

        .ev-card-actions {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .btn-ev-action {
          display: flex;
          align-items: center;
          gap: 4px;
          background: transparent;
          border: 1px solid var(--border-color);
          border-radius: 3px;
          padding: 3px 8px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-ev-action.view {
          color: var(--text-bright);
        }

        .btn-ev-action.view:hover {
          border-color: var(--bright-crime-red);
          background-color: rgba(193, 18, 31, 0.1);
        }

        .btn-ev-action.download {
          color: #10b981;
          border-color: rgba(16, 185, 129, 0.4);
        }

        .btn-ev-action.download:hover {
          background-color: rgba(16, 185, 129, 0.15);
          border-color: #10b981;
        }

        .reg-time {
          font-family: var(--font-mono);
          font-size: 0.68rem;
          color: var(--text-muted);
        }

        .btn-edit-subtle {
          display: flex;
          align-items: center;
          gap: 4px;
          background: transparent;
          border: 1px solid var(--border-color);
          border-radius: 3px;
          padding: 3px 8px;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          cursor: pointer;
        }

        .btn-edit-subtle:hover {
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        /* Evidence Detail Modal Elements */
        .ev-detail-banner {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          padding: 16px;
          background-color: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-radius: 4px;
        }

        .ev-banner-title-col {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .ev-detail-title {
          font-size: 1.15rem;
          font-weight: 700;
          color: var(--text-bright);
          margin: 0;
        }

        .ev-filename {
          font-family: var(--font-mono);
          font-size: 0.75rem;
          color: var(--text-muted);
        }

        .ev-banner-actions {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          gap: 8px;
        }

        .btn-download-prominent {
          display: flex;
          align-items: center;
          gap: 6px;
          background: linear-gradient(180deg, #10b981 0%, #059669 100%);
          border: 1px solid #10b981;
          border-radius: 4px;
          padding: 6px 14px;
          color: white;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          cursor: pointer;
          box-shadow: 0 0 10px rgba(16, 185, 129, 0.25);
        }

        .btn-download-prominent:hover {
          filter: brightness(1.1);
        }

        .hash-box-panel {
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 12px 16px;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .hash-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .hash-label {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          color: var(--text-muted);
        }

        .btn-copy-hash {
          display: flex;
          align-items: center;
          gap: 4px;
          background: transparent;
          border: 1px solid var(--border-color);
          border-radius: 3px;
          padding: 2px 8px;
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          color: var(--text-secondary);
          cursor: pointer;
        }

        .btn-copy-hash:hover {
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        .mono-hash-full {
          font-family: var(--font-mono);
          font-size: 0.78rem;
          color: #10b981;
          word-break: break-all;
        }

        .status-transition-box {
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 10px;
          background-color: rgba(18, 18, 20, 0.9);
          border: 1px solid var(--border-color);
        }

        .transition-title {
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
        }

        .transition-form {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .transition-action-row {
          display: flex;
          justify-content: flex-end;
        }

        .btn-update-status {
          display: flex;
          align-items: center;
          gap: 6px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 6px 14px;
          color: white;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          cursor: pointer;
        }

        .audit-trail-section {
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .audit-trail-title {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
        }

        .audit-logs-list {
          display: flex;
          flex-direction: column;
          gap: 6px;
          max-height: 180px;
          overflow-y: auto;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 8px;
        }

        .audit-log-row {
          display: grid;
          grid-template-columns: 140px 140px 140px 1fr;
          gap: 8px;
          font-size: 0.72rem;
          padding: 6px 8px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.4);
        }

        .audit-log-row:last-child {
          border-bottom: none;
        }

        .audit-time {
          font-family: var(--font-mono);
          color: var(--text-muted);
        }

        .audit-action-tag {
          color: var(--bright-crime-red);
          font-weight: 700;
        }

        .audit-officer {
          color: var(--text-secondary);
        }

        .audit-details {
          color: var(--text-primary);
        }

        .audit-empty {
          font-size: 0.75rem;
          color: var(--text-muted);
          padding: 8px;
        }

        /* Upload File Drop Zone */
        .file-drop-zone {
          border: 2px dashed var(--border-color);
          border-radius: 4px;
          background-color: var(--bg-primary);
          transition: border-color var(--transition-fast);
        }

        .file-drop-zone:hover {
          border-color: var(--bright-crime-red);
        }

        .file-hidden-input {
          display: none;
        }

        .file-drop-label {
          padding: 28px 16px;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          text-align: center;
        }

        .drop-icon {
          color: var(--bright-crime-red);
        }

        .drop-title {
          font-family: var(--font-subheading);
          font-size: 0.85rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .drop-subtitle {
          font-size: 0.72rem;
          color: var(--text-muted);
        }

        .upload-progress-container {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .progress-label-row {
          display: flex;
          justify-content: space-between;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          color: var(--text-muted);
        }

        .progress-track {
          width: 100%;
          height: 6px;
          background-color: var(--bg-primary);
          border-radius: 3px;
          overflow: hidden;
          border: 1px solid var(--border-color);
        }

        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, var(--primary-crimson) 0%, var(--bright-crime-red) 100%);
          transition: width 0.2s ease-in-out;
        }

        .modal-error-alert {
          display: flex;
          align-items: center;
          gap: 8px;
          background-color: rgba(193, 18, 31, 0.18);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 8px 12px;
          color: #ff4d4d;
          font-size: 0.78rem;
          font-weight: 600;
        }

        /* Classified Modules Tab */
        .future-modules-panel {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          padding: 20px;
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .future-header {
          display: flex;
          align-items: center;
          gap: 8px;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.12em;
          color: var(--text-muted);
          padding-bottom: 10px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.6);
        }

        .future-lock { color: var(--bright-crime-red); }

        .modules-list {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .module-item {
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 14px;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .module-item.restricted { opacity: 0.75; }
        .module-item.active-phase { border-color: rgba(16, 185, 129, 0.5); background-color: rgba(16, 185, 129, 0.04); }

        .module-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .module-info {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .mod-icon { color: var(--bright-crime-red); }

        .mod-name {
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          letter-spacing: 0.06em;
          color: var(--text-bright);
        }

        .phase-badge {
          font-family: var(--font-mono);
          font-size: 0.62rem;
          color: var(--text-muted);
          background-color: rgba(42, 42, 45, 0.6);
          padding: 2px 6px;
          border-radius: 2px;
        }

        .phase-badge-active {
          font-family: var(--font-mono);
          font-size: 0.62rem;
          color: #10b981;
          background-color: rgba(16, 185, 129, 0.15);
          border: 1px solid #10b981;
          padding: 2px 6px;
          border-radius: 2px;
        }

        .mod-desc {
          font-size: 0.75rem;
          color: var(--text-muted);
          padding-left: 24px;
        }

        /* Priority and Status Badges */
        .priority-badge, .status-badge {
          display: inline-block;
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          padding: 3px 8px;
          border-radius: 3px;
        }

        .badge-priority-critical { background-color: rgba(193, 18, 31, 0.25); border: 1px solid var(--bright-crime-red); color: #ff4d4d; box-shadow: 0 0 8px rgba(193, 18, 31, 0.3); }
        .badge-priority-high { background-color: rgba(139, 0, 0, 0.25); border: 1px solid var(--primary-crimson); color: #ff858d; }
        .badge-priority-medium { background-color: rgba(255, 165, 0, 0.15); border: 1px solid #ffaa00; color: #ffc107; }
        .badge-priority-low { background-color: rgba(100, 116, 139, 0.15); border: 1px solid #64748b; color: #94a3b8; }

        .badge-status-open { background-color: rgba(0, 180, 216, 0.15); border: 1px solid #00b4d8; color: #48cae4; }
        .badge-status-investigating { background-color: rgba(255, 170, 0, 0.15); border: 1px solid #ffaa00; color: #ffaa00; }
        .badge-status-review { background-color: rgba(234, 88, 12, 0.15); border: 1px solid #ea580c; color: #fb923c; }
        .badge-status-hold { background-color: rgba(147, 51, 234, 0.15); border: 1px solid #9333ea; color: #c084fc; }
        .badge-status-closed { background-color: rgba(71, 85, 105, 0.2); border: 1px solid #475569; color: #94a3b8; }

        .loading-state-box, .error-state-card {
          padding: 60px 20px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 16px;
          text-align: center;
        }

        .error-state-card {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          max-width: 500px;
          margin: 40px auto;
        }

        .err-icon { color: var(--bright-crime-red); }
        .err-title { font-family: var(--font-subheading); font-size: 1rem; font-weight: 700; color: var(--text-bright); }
        .err-desc { font-size: 0.8rem; color: var(--text-muted); }

        .btn-back-dir {
          display: flex;
          align-items: center;
          gap: 6px;
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 8px 16px;
          color: white;
          font-family: var(--font-subheading);
          font-size: 0.78rem;
          font-weight: 700;
          cursor: pointer;
        }

        /* Modals styling */
        .modal-backdrop {
          position: fixed;
          top: 0;
          left: 0;
          width: 100vw;
          height: 100vh;
          background-color: rgba(0, 0, 0, 0.85);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
          padding: 20px;
        }

        .edit-modal {
          width: 100%;
          max-width: 760px;
          max-height: 90vh;
          overflow-y: auto;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          box-shadow: 0 16px 48px rgba(0, 0, 0, 0.9);
        }

        .modal-header {
          padding: 16px 20px;
          background-color: var(--bg-panel-elevated);
          border-bottom: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: space-between;
          position: sticky;
          top: 0;
          z-index: 10;
        }

        .modal-title-row {
          display: flex;
          align-items: center;
          gap: 8px;
          font-family: var(--font-subheading);
          font-size: 0.85rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-bright);
        }

        .modal-title-icon { color: var(--bright-crime-red); }

        .btn-close-modal {
          background: transparent;
          border: none;
          color: var(--text-muted);
          cursor: pointer;
        }

        .btn-close-modal:hover { color: var(--bright-crime-red); }

        .modal-form {
          padding: 20px;
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .form-row-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 14px;
        }

        .form-row-grid-3 {
          display: grid;
          grid-template-columns: 1fr 1fr 1fr;
          gap: 14px;
        }

        .form-group {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .form-label {
          color: var(--text-secondary);
          font-size: 0.72rem;
        }

        .req { color: var(--bright-crime-red); }

        .case-input, .case-select, .case-textarea {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 8px 10px;
          color: var(--text-bright);
          font-size: 0.85rem;
          outline: none;
        }

        .case-input:focus, .case-select:focus, .case-textarea:focus {
          border-color: var(--bright-crime-red);
        }

        .modal-actions {
          display: flex;
          align-items: center;
          justify-content: flex-end;
          gap: 12px;
          margin-top: 10px;
        }

        .btn-cancel {
          background: transparent;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 8px 16px;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          cursor: pointer;
        }

        .btn-save-edit {
          display: flex;
          align-items: center;
          gap: 6px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 8px 18px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 700;
          cursor: pointer;
        }

        /* Phase 13: Notification Audit & Test Alert Styles */
        .notif-tab-container {
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .notif-main-panel {
          padding: 24px;
        }

        .notif-header-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          flex-wrap: wrap;
          gap: 16px;
          border-bottom: 1px solid var(--border-color);
          padding-bottom: 16px;
          margin-bottom: 20px;
        }

        .notif-title-group {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .notif-title-icon-box {
          padding: 10px;
          border-radius: 8px;
          background: rgba(139, 0, 0, 0.2);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: var(--bright-crime-red);
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .notif-title {
          font-family: var(--font-heading);
          font-size: 1.1rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          color: var(--text-bright);
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .notif-badge-phase {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          padding: 2px 8px;
          border-radius: 4px;
          background: rgba(193, 18, 31, 0.2);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: var(--bright-crime-red);
          font-weight: 700;
          letter-spacing: 0.1em;
        }

        .notif-subtitle {
          font-size: 0.75rem;
          color: var(--text-secondary);
          margin-top: 2px;
        }

        .notif-header-actions {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .btn-notif-refresh {
          display: flex;
          align-items: center;
          gap: 6px;
          padding: 6px 12px;
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-notif-refresh:hover {
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        .btn-notif-trigger {
          display: flex;
          align-items: center;
          gap: 6px;
          padding: 6px 14px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.78rem;
          font-weight: 700;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-notif-trigger:hover {
          opacity: 0.9;
          box-shadow: var(--red-glow-subtle);
        }

        .spin-icon {
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }

        .notif-kpi-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
          gap: 12px;
          margin-bottom: 20px;
        }

        .notif-kpi-card {
          padding: 12px;
          border-radius: 4px;
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .notif-kpi-card.kpi-card-delivered {
          border-color: rgba(16, 185, 129, 0.3);
          background: rgba(16, 185, 129, 0.05);
        }

        .notif-kpi-card.kpi-card-failed {
          border-color: rgba(193, 18, 31, 0.3);
          background: rgba(193, 18, 31, 0.05);
        }

        .notif-kpi-card.kpi-card-email {
          border-color: rgba(59, 130, 246, 0.3);
          background: rgba(59, 130, 246, 0.05);
        }

        .notif-kpi-card.kpi-card-whatsapp {
          border-color: rgba(34, 197, 94, 0.3);
          background: rgba(34, 197, 94, 0.05);
        }

        .notif-kpi-label {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          color: var(--text-muted);
          font-weight: 700;
          letter-spacing: 0.08em;
        }

        .notif-kpi-val {
          font-family: var(--font-heading);
          font-size: 1.35rem;
          font-weight: 700;
          color: var(--text-bright);
        }

        .notif-kpi-val.val-green { color: #10B981; }
        .notif-kpi-val.val-red { color: var(--bright-crime-red); }
        .notif-kpi-val.val-blue { color: #3B82F6; }
        .notif-kpi-val.val-whatsapp { color: #22C55E; }

        .notif-loading-box, .notif-empty-box {
          padding: 40px 20px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 10px;
          border: 1px dashed var(--border-color);
          border-radius: 6px;
        }

        .notif-empty-icon {
          color: var(--text-muted);
          opacity: 0.5;
        }

        .notif-empty-title {
          font-family: var(--font-heading);
          font-size: 0.95rem;
          color: var(--text-secondary);
        }

        .notif-empty-desc {
          font-size: 0.75rem;
          color: var(--text-muted);
          max-width: 420px;
        }

        .btn-notif-empty-trigger {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-top: 8px;
          padding: 7px 14px;
          background: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          color: var(--text-primary);
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          cursor: pointer;
        }

        .btn-notif-empty-trigger:hover {
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        .notif-cards-list {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .notif-dispatch-card {
          padding: 16px;
          border-radius: 4px;
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          display: flex;
          flex-direction: column;
          gap: 8px;
          transition: border-color var(--transition-fast);
        }

        .notif-dispatch-card:hover {
          border-color: rgba(193, 18, 31, 0.4);
        }

        .notif-card-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          flex-wrap: wrap;
          gap: 8px;
        }

        .notif-card-badges {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 8px;
        }

        .channel-pill {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          padding: 2px 8px;
          border-radius: 4px;
        }

        .channel-pill.pill-email {
          background: rgba(59, 130, 246, 0.15);
          border: 1px solid rgba(59, 130, 246, 0.35);
          color: #60A5FA;
        }

        .channel-pill.pill-whatsapp {
          background: rgba(34, 197, 94, 0.15);
          border: 1px solid rgba(34, 197, 94, 0.35);
          color: #4ADE80;
        }

        .event-type-badge {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          color: var(--text-primary);
          background: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          padding: 2px 8px;
          border-radius: 4px;
          letter-spacing: 0.05em;
        }

        .recipient-badge {
          font-size: 0.75rem;
          color: var(--text-secondary);
          font-family: monospace;
        }

        .notif-card-status-row {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .status-pill {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          padding: 2px 8px;
          border-radius: 4px;
        }

        .status-pill.pill-sent {
          background: rgba(16, 185, 129, 0.15);
          border: 1px solid rgba(16, 185, 129, 0.35);
          color: #10B981;
        }

        .status-pill.pill-failed {
          background: rgba(193, 18, 31, 0.15);
          border: 1px solid rgba(193, 18, 31, 0.35);
          color: var(--bright-crime-red);
        }

        .status-pill.pill-skipped {
          background: rgba(100, 116, 139, 0.15);
          border: 1px solid rgba(100, 116, 139, 0.35);
          color: #94A3B8;
        }

        .status-pill.pill-pending {
          background: rgba(245, 158, 11, 0.15);
          border: 1px solid rgba(245, 158, 11, 0.35);
          color: #F59E0B;
        }

        .status-pill.pill-mock_sent {
          background: rgba(16, 185, 129, 0.2);
          border: 1px solid rgba(16, 185, 129, 0.4);
          color: #10B981;
        }

        .notif-timestamp {
          font-size: 0.7rem;
          color: var(--text-muted);
          font-family: monospace;
        }

        .notif-subject-line {
          font-size: 0.82rem;
          font-weight: 600;
          color: var(--text-bright);
        }

        .notif-body-preview {
          font-size: 0.75rem;
          color: var(--text-secondary);
          background: #0d0d0f;
          padding: 10px 12px;
          border-radius: 4px;
          border: 1px solid #1e1e22;
          white-space: pre-wrap;
          word-break: break-word;
          line-height: 1.5;
        }

        .notif-failure-banner {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          padding: 8px 12px;
          border-radius: 4px;
          background: rgba(193, 18, 31, 0.1);
          border: 1px solid rgba(193, 18, 31, 0.3);
          color: var(--bright-crime-red);
          font-size: 0.75rem;
        }

        .failure-icon {
          flex-shrink: 0;
          margin-top: 2px;
        }

        .failure-label {
          font-weight: 700;
        }

        .retry-counter-tag {
          margin-left: 8px;
          font-family: monospace;
          font-size: 0.7rem;
          opacity: 0.8;
        }

        .test-modal-desc {
          font-size: 0.78rem;
          color: var(--text-secondary);
          line-height: 1.4;
          margin-top: -4px;
        }

        .notif-input-disabled {
          opacity: 0.7;
          cursor: not-allowed;
          background: rgba(255, 255, 255, 0.03);
        }

        .notif-policy-note {
          padding: 10px 12px;
          border-radius: 4px;
          background: #0d0d0f;
          border: 1px solid var(--border-color);
          font-size: 0.72rem;
          color: var(--text-muted);
          line-height: 1.4;
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .policy-note-title {
          display: flex;
          align-items: center;
          gap: 6px;
          color: var(--text-secondary);
          font-weight: 600;
        }

        .policy-code {
          color: var(--text-bright);
          background: rgba(255, 255, 255, 0.06);
          padding: 1px 4px;
          border-radius: 3px;
        }

        .notif-modal-feedback {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px 12px;
          border-radius: 4px;
          font-size: 0.78rem;
        }

        .notif-modal-feedback.success {
          background: rgba(16, 185, 129, 0.15);
          border: 1px solid rgba(16, 185, 129, 0.35);
          color: #10B981;
        }

        .notif-modal-feedback.error {
          background: rgba(193, 18, 31, 0.15);
          border: 1px solid rgba(193, 18, 31, 0.35);
          color: var(--bright-crime-red);
        }

        /* Phase 14: Public Intelligence & News Search Styles */
        .public-intel-tab-container {
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .intel-disclaimer-banner {
          background: rgba(234, 88, 12, 0.08);
          border: 1px solid rgba(234, 88, 12, 0.35);
          border-radius: 6px;
          padding: 16px 20px;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .disclaimer-header {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .disclaimer-icon {
          color: #fb923c;
          flex-shrink: 0;
        }

        .disclaimer-title {
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: #fb923c;
        }

        .disclaimer-text {
          font-size: 0.82rem;
          line-height: 1.5;
          color: var(--text-primary);
        }

        .disclaimer-subtext {
          font-size: 0.72rem;
          color: var(--text-muted);
          font-style: italic;
        }

        .public-search-main-panel,
        .saved-intel-panel {
          padding: 20px;
          display: flex;
          flex-direction: column;
          gap: 16px;
        }

        .intel-header-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          flex-wrap: wrap;
          gap: 10px;
          padding-bottom: 12px;
          border-bottom: 1px solid var(--border-color);
        }

        .intel-title-group {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .intel-panel-title {
          font-family: var(--font-subheading);
          font-size: 0.92rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .intel-panel-subtitle {
          font-size: 0.75rem;
          color: var(--text-muted);
        }

        .suggested-queries-box {
          display: flex;
          flex-direction: column;
          gap: 8px;
          background: var(--bg-primary);
          padding: 12px 14px;
          border-radius: 4px;
          border: 1px solid var(--border-color);
        }

        .suggested-label-row {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          color: var(--text-secondary);
          letter-spacing: 0.06em;
        }

        .suggested-chips-list {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
        }

        .suggested-query-chip {
          background: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 5px 10px;
          font-size: 0.74rem;
          color: var(--text-primary);
          cursor: pointer;
          transition: all var(--transition-fast);
          display: inline-flex;
          align-items: center;
          gap: 6px;
        }

        .suggested-query-chip:hover {
          border-color: var(--bright-crime-red);
          color: var(--bright-crime-red);
          background: rgba(193, 18, 31, 0.08);
        }

        .public-search-form {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .search-controls-row {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }

        .search-input-wrapper {
          flex: 1;
          min-width: 260px;
          position: relative;
          display: flex;
          align-items: center;
        }

        .search-input-icon {
          position: absolute;
          left: 12px;
          color: var(--text-muted);
          pointer-events: none;
        }

        .search-text-input {
          width: 100%;
          padding: 9px 12px 9px 36px;
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          color: var(--text-bright);
          font-size: 0.82rem;
          transition: border-color var(--transition-fast);
        }

        .search-text-input:focus {
          border-color: var(--bright-crime-red);
          outline: none;
        }

        .search-type-select {
          padding: 9px 12px;
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          color: var(--text-primary);
          font-family: var(--font-subheading);
          font-size: 0.76rem;
          font-weight: 700;
          cursor: pointer;
        }

        .btn-intel-search {
          display: flex;
          align-items: center;
          gap: 8px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 9px 18px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.82rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
          box-shadow: var(--red-glow-subtle);
        }

        .btn-intel-search:hover:not(:disabled) {
          box-shadow: var(--red-glow-medium);
          transform: translateY(-1px);
        }

        .btn-intel-search:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .date-filters-row {
          display: flex;
          align-items: center;
          gap: 16px;
          flex-wrap: wrap;
          font-size: 0.75rem;
          color: var(--text-secondary);
        }

        .date-filter-item {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .date-input {
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          color: var(--text-primary);
          padding: 5px 8px;
          font-size: 0.74rem;
        }

        .intel-error-banner {
          background: rgba(193, 18, 31, 0.12);
          border: 1px solid rgba(193, 18, 31, 0.35);
          border-radius: 4px;
          padding: 10px 14px;
          color: var(--bright-crime-red);
          font-size: 0.78rem;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .public-results-section {
          display: flex;
          flex-direction: column;
          gap: 14px;
          margin-top: 8px;
        }

        .results-section-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 0.75rem;
          color: var(--text-secondary);
        }

        .results-cards-grid, .results-cards-list {
          display: flex;
          flex-direction: column;
          gap: 16px;
          width: 100%;
          min-width: 0;
          max-width: 100%;
          box-sizing: border-box;
        }

        .public-news-card, .public-result-card {
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
          overflow: hidden;
          height: auto;
          min-height: 0;
          display: grid;
          grid-template-columns: 120px minmax(0, 1fr);
          gap: 20px;
          align-items: start;
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          padding: 16px;
          transition: border-color var(--transition-fast);
        }

        .public-news-card:hover, .public-result-card:hover {
          border-color: rgba(193, 18, 31, 0.4);
        }

        .public-news-meta, .result-card-header {
          min-width: 0;
          width: 100%;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .public-news-source,
        .public-news-date,
        .public-news-author,
        .source-date,
        .source-author {
          max-width: 100%;
          overflow-wrap: anywhere;
          word-break: break-word;
          font-size: 0.74rem;
          color: var(--text-muted);
        }

        .public-news-content, .result-card-body {
          min-width: 0;
          max-width: 100%;
          width: 100%;
          box-sizing: border-box;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .public-news-title, .result-title {
          min-width: 0;
          max-width: 100%;
          overflow-wrap: anywhere;
          word-break: break-word;
          white-space: normal;
          line-height: 1.25;
          font-size: 1rem;
          font-weight: 700;
          color: var(--text-bright);
          margin: 0;
        }

        .public-news-description, .result-description {
          max-width: 100%;
          overflow-wrap: anywhere;
          word-break: break-word;
          white-space: normal;
          line-height: 1.5;
          font-size: 0.82rem;
          color: var(--text-secondary);
          margin: 0;
        }

        .public-news-media, .result-img-wrapper {
          width: 100%;
          max-width: 100%;
          margin-top: 12px;
          border-radius: 8px;
          overflow: hidden;
          box-sizing: border-box;
          background: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
        }

        .public-news-media img, .result-thumbnail {
          display: block;
          width: 100%;
          max-width: 100%;
          height: 320px;
          object-fit: cover;
        }

        .public-news-video {
          display: block;
          width: 100%;
          max-width: 100%;
          height: 320px;
          object-fit: cover;
          border-radius: 8px;
          background: #000;
        }

        .public-news-no-media {
          padding: 24px;
          background: var(--bg-panel-elevated, #17171A);
          border: 1px dashed var(--border-color, #2A2A2D);
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .no-media-placeholder {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 0.74rem;
          color: var(--text-muted, #6F6F73);
          font-family: var(--font-mono, monospace);
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }

        .public-news-verification, .unverified-chip {
          align-self: flex-start;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          margin-top: 8px;
          padding: 3px 8px;
          border-radius: 4px;
          font-family: var(--font-mono, monospace);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          text-transform: uppercase;
          background: rgba(245, 158, 11, 0.12);
          border: 1px solid rgba(245, 158, 11, 0.35);
          color: #FBBF24;
          max-width: 100%;
          overflow-wrap: anywhere;
        }

        .public-news-actions, .result-card-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-top: 12px;
          padding-top: 10px;
          border-top: 1px solid rgba(42, 42, 45, 0.5);
          flex-wrap: wrap;
          width: 100%;
          box-sizing: border-box;
        }

        @media (max-width: 768px) {
          .public-news-card, .public-result-card {
            grid-template-columns: 1fr;
            gap: 12px;
          }

          .public-news-meta, .result-card-header {
            display: flex;
            flex-direction: row;
            flex-wrap: wrap;
            gap: 8px 12px;
            align-items: center;
          }

          .public-news-media img,
          .result-thumbnail,
          .public-news-video {
            height: 220px;
          }
        }

        .result-source-meta {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 10px;
          font-size: 0.76rem;
          color: var(--text-secondary);
        }

        .source-name-badge {
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          color: #60A5FA;
          background: rgba(59, 130, 246, 0.12);
          border: 1px solid rgba(59, 130, 246, 0.3);
          padding: 2px 8px;
          border-radius: 4px;
        }

        .source-date, .source-author {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: 0.74rem;
          color: var(--text-muted);
        }

        .saved-intel-title-row {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }

        .saved-intel-heading {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }

        .saved-title-text {
          font-family: var(--font-subheading);
          font-size: 0.88rem;
          font-weight: 700;
          letter-spacing: 0.06em;
          color: var(--text-bright);
        }

        .saved-count-badge {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          color: var(--bright-crime-red);
          background: rgba(193, 18, 31, 0.15);
          border: 1px solid rgba(193, 18, 31, 0.4);
          padding: 2px 8px;
          border-radius: 10px;
        }

        .result-date-tag {
          font-size: 0.7rem;
          color: var(--text-muted);
          font-family: monospace;
        }

        .result-title-link {
          font-size: 0.95rem;
          font-weight: 700;
          color: var(--text-bright);
          text-decoration: none;
          line-height: 1.35;
          transition: color var(--transition-fast);
        }

        .result-title-link:hover {
          color: var(--bright-crime-red);
        }

        .result-snippet {
          font-size: 0.78rem;
          color: var(--text-secondary);
          line-height: 1.5;
        }

        .result-card-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-top: 4px;
          padding-top: 8px;
          border-top: 1px solid rgba(42, 42, 45, 0.5);
        }

        .btn-open-source {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          color: var(--text-secondary);
          text-decoration: none;
          padding: 5px 10px;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          background: var(--bg-panel-elevated);
          transition: all var(--transition-fast);
        }

        .btn-open-source:hover {
          border-color: var(--text-bright);
          color: var(--text-bright);
        }

        .btn-open-source.mock-disabled, .btn-saved-open.mock-disabled {
          color: var(--text-muted, #9CA3AF);
          border-color: rgba(156, 163, 175, 0.3);
          background: rgba(255, 255, 255, 0.03);
          cursor: not-allowed;
          font-style: italic;
          opacity: 0.85;
        }

        .btn-save-public {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          color: var(--bright-crime-red);
          padding: 5px 12px;
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          background: rgba(193, 18, 31, 0.1);
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-save-public:hover:not(:disabled) {
          background: var(--bright-crime-red);
          color: white;
        }

        .btn-save-public.saved {
          color: #10B981;
          border-color: #10B981;
          background: rgba(16, 185, 129, 0.12);
          cursor: default;
        }

        /* Saved Intelligence List & Cards */
        .saved-items-list {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .saved-item-card {
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .saved-item-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          flex-wrap: wrap;
          gap: 8px;
        }

        .saved-item-meta {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 8px;
        }

        .intel-status-pill {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          padding: 3px 8px;
          border-radius: 4px;
        }

        .intel-status-unverified {
          background: rgba(100, 116, 139, 0.18);
          border: 1px solid rgba(100, 116, 139, 0.4);
          color: #94A3B8;
        }

        .intel-status-under_review {
          background: rgba(245, 158, 11, 0.15);
          border: 1px solid rgba(245, 158, 11, 0.4);
          color: #F59E0B;
        }

        .intel-status-corroborated {
          background: rgba(16, 185, 129, 0.18);
          border: 1px solid rgba(16, 185, 129, 0.4);
          color: #10B981;
        }

        .intel-status-contradicted {
          background: rgba(193, 18, 31, 0.18);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: var(--bright-crime-red);
        }

        .intel-status-not_relevant {
          background: rgba(71, 85, 105, 0.15);
          border: 1px solid rgba(71, 85, 105, 0.35);
          color: #64748B;
        }

        .saved-item-actions {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .btn-saved-open {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          color: var(--text-secondary);
          padding: 4px 8px;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          background: var(--bg-panel-elevated);
          text-decoration: none;
        }

        .btn-saved-open:hover {
          color: var(--text-bright);
          border-color: var(--text-bright);
        }

        .btn-saved-review {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          color: var(--bright-crime-red);
          padding: 4px 10px;
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          background: rgba(193, 18, 31, 0.08);
          cursor: pointer;
        }

        .btn-saved-review:hover {
          background: var(--bright-crime-red);
          color: white;
        }

        .btn-saved-delete {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 5px 8px;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          background: transparent;
          color: var(--text-muted);
          cursor: pointer;
        }

        .btn-saved-delete:hover {
          border-color: var(--bright-crime-red);
          color: var(--bright-crime-red);
        }

        .saved-item-body {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .saved-item-title {
          font-size: 0.9rem;
          font-weight: 700;
          color: var(--text-bright);
          margin: 0;
        }

        .saved-item-desc {
          font-size: 0.78rem;
          color: var(--text-secondary);
          line-height: 1.5;
          margin: 0;
        }

        .investigator-notes-box {
          background: #0b0b0d;
          border: 1px solid rgba(234, 88, 12, 0.3);
          border-left: 3px solid #f59e0b;
          border-radius: 4px;
          padding: 8px 12px;
          display: flex;
          flex-direction: column;
          gap: 4px;
          margin-top: 4px;
        }

        .notes-label {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: #f59e0b;
        }

        .notes-text {
          font-size: 0.76rem;
          color: var(--text-primary);
          line-height: 1.45;
          margin: 0;
          white-space: pre-wrap;
        }

        .saved-item-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 0.7rem;
          color: var(--text-muted);
          padding-top: 8px;
          border-top: 1px solid rgba(42, 42, 45, 0.4);
        }

        @media (max-width: 1100px) {
          .dossier-grid { grid-template-columns: 1fr; }
          .metadata-card { grid-template-columns: 1fr; }
          .fir-details-grid { grid-template-columns: 1fr; }
          .form-row-grid, .form-row-grid-3 { grid-template-columns: 1fr; }
          .audit-log-row { grid-template-columns: 1fr; }
          .ev-detail-banner { flex-direction: column; gap: 12px; }
          .ev-banner-actions { align-items: flex-start; }
        }

        /* INVESTIGATION TEAM SECTION STYLES */
        .investigation-team-wrapper {
          margin-top: 24px;
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
          overflow: visible;
        }

        .investigation-team-grid {
          display: grid;
          grid-template-columns: repeat(3, minmax(0, 1fr));
          gap: 20px;
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
        }

        @media (max-width: 1200px) {
          .investigation-team-grid {
            grid-template-columns: repeat(2, minmax(0, 1fr));
          }
        }

        @media (max-width: 768px) {
          .investigation-team-grid {
            grid-template-columns: 1fr;
          }
        }

        .officer-card {
          background-color: #11161f;
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 8px;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 14px;
          transition: border-color 0.2s ease, box-shadow 0.2s ease;
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
          overflow: visible;
        }

        .officer-card:hover {
          border-color: rgba(193, 18, 31, 0.5);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
        }

        .primary-officer-card {
          background: linear-gradient(180deg, rgba(153, 27, 27, 0.18) 0%, rgba(17, 22, 31, 0.95) 100%);
          border: 1px solid rgba(185, 28, 28, 0.6);
          box-shadow: 0 4px 16px rgba(153, 27, 27, 0.2);
        }

        .officer-header {
          display: flex;
          align-items: center;
          gap: 12px;
          padding-bottom: 10px;
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
          width: 100%;
          min-width: 0;
          box-sizing: border-box;
        }

        .officer-avatar-box {
          width: 38px;
          height: 38px;
          border-radius: 50%;
          background-color: rgba(255, 255, 255, 0.06);
          display: flex;
          align-items: center;
          justify-content: center;
          color: #d1d5db;
          flex-shrink: 0;
        }

        .primary-officer-card .officer-avatar-box {
          background-color: rgba(185, 28, 28, 0.3);
          color: #fca5a5;
          border: 1px solid rgba(185, 28, 28, 0.5);
        }

        .officer-identity {
          display: flex;
          flex-direction: column;
          gap: 2px;
          min-width: 0;
          flex: 1;
        }

        .officer-name {
          font-size: 1.05rem;
          font-weight: 700;
          color: #ffffff;
          margin: 0;
          line-height: 1.3;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .officer-id {
          font-size: 0.75rem;
          font-family: var(--font-mono, monospace);
          color: #9ca3af;
          margin: 0;
        }

        .officer-badges {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
          width: 100%;
          min-width: 0;
        }

        .assignment-badge {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 4px 10px;
          border-radius: 4px;
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          text-transform: uppercase;
        }

        .primary-badge {
          background-color: rgba(153, 27, 27, 0.7);
          color: #fecaca;
          border: 1px solid rgba(220, 38, 38, 0.8);
        }

        .collaborating-badge {
          background-color: rgba(31, 41, 55, 0.9);
          color: #d1d5db;
          border: 1px solid rgba(75, 85, 99, 0.8);
        }

        .status-badge {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 4px 10px;
          border-radius: 4px;
          font-size: 0.68rem;
          font-weight: 600;
          letter-spacing: 0.05em;
          text-transform: uppercase;
        }

        .active-badge {
          background-color: rgba(6, 78, 59, 0.7);
          color: #6ee7b7;
          border: 1px solid rgba(16, 185, 129, 0.7);
        }

        .inactive-badge {
          background-color: rgba(31, 41, 55, 0.8);
          color: #9ca3af;
          border: 1px solid rgba(75, 85, 99, 0.6);
        }

        .pulse-green-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background-color: #34d399;
          box-shadow: 0 0 6px #34d399;
          display: inline-block;
        }

        .officer-meta-list {
          display: flex;
          flex-direction: column;
          gap: 8px;
          font-size: 0.8rem;
          padding-top: 4px;
          border-top: 1px solid rgba(255, 255, 255, 0.06);
          width: 100%;
          min-width: 0;
          box-sizing: border-box;
        }

        .officer-meta {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12px;
          width: 100%;
          min-width: 0;
          box-sizing: border-box;
        }

        .meta-label {
          color: #9ca3af;
          font-weight: 500;
          display: flex;
          align-items: center;
          gap: 6px;
          white-space: nowrap;
          flex-shrink: 0;
        }

        .meta-value {
          color: #e5e7eb;
          font-weight: 600;
          text-align: right;
          word-break: break-word;
          overflow-wrap: anywhere;
          min-width: 0;
        }

        .team-loading-state {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 32px;
          color: #9ca3af;
          gap: 8px;
        }

        .team-empty-state {
          padding: 32px;
          text-align: center;
          color: #9ca3af;
          background-color: rgba(17, 22, 31, 0.5);
          border-radius: 8px;
          border: 1px solid #1f2937;
        }

        .team-empty-state .empty-icon {
          margin: 0 auto 8px auto;
          color: #6b7280;
        }

        .team-empty-state .empty-title {
          font-weight: 600;
          color: #d1d5db;
          margin: 0 0 4px 0;
        }

        .team-empty-state .empty-desc {
          font-size: 0.85rem;
          color: #6b7280;
          margin: 0;
        }

        /* Report Tab Styles */
        .report-gen-panel {
          padding: 24px;
        }
        .report-hero {
          display: flex;
          align-items: center;
          gap: 20px;
          background: #181c26;
          border: 1px solid #283042;
          border-radius: 8px;
          padding: 24px;
          flex-wrap: wrap;
        }
        .report-hero-icon-wrap {
          background: rgba(193, 18, 31, 0.15);
          border: 1px solid rgba(193, 18, 31, 0.3);
          border-radius: 8px;
          padding: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .report-hero-info {
          flex-grow: 1;
          min-width: 280px;
        }
        .report-hero-title {
          font-family: var(--font-heading, monospace);
          font-size: 1.1rem;
          font-weight: 700;
          color: #f1f3f9;
          margin: 0 0 6px 0;
          letter-spacing: 0.06em;
        }
        .report-hero-desc {
          font-size: 0.8rem;
          color: #8c93a8;
          margin: 0;
          line-height: 1.45;
          max-width: 600px;
        }
        .btn-download-pdf-large {
          display: flex;
          align-items: center;
          gap: 8px;
          background: #c1121f;
          color: #fff;
          border: none;
          padding: 12px 20px;
          border-radius: 6px;
          font-size: 0.82rem;
          font-weight: 700;
          cursor: pointer;
          transition: all 0.15s;
          white-space: nowrap;
        }
        .btn-download-pdf-large:hover {
          background: #e63946;
          transform: translateY(-1px);
        }
        .btn-download-pdf-large:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
        .report-summary-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
          gap: 14px;
        }
        .report-summary-card {
          background: #181c26;
          border: 1px solid #232733;
          border-radius: 6px;
          padding: 14px 16px;
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .sum-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          color: #8c93a8;
          letter-spacing: 0.08em;
        }
        .sum-val {
          font-family: var(--font-mono, monospace);
          font-size: 1.25rem;
          font-weight: 800;
        }
        .sum-sub {
          font-size: 0.72rem;
          color: #64748b;
        }
        .report-history-section {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 6px;
          padding: 16px 20px;
        }
        .history-header {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .history-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.76rem;
          font-weight: 700;
          color: #f1f3f9;
          letter-spacing: 0.08em;
        }
        .history-table-wrap {
          overflow-x: auto;
          border: 1px solid #232733;
          border-radius: 4px;
        }
        .history-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 0.78rem;
        }
        .history-table th {
          background: #181c26;
          color: #8c93a8;
          padding: 10px 12px;
          text-align: left;
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          border-bottom: 1px solid #232733;
        }
        .history-table td {
          padding: 10px 12px;
          border-bottom: 1px solid #1c202a;
          color: #d1d5db;
        }
        .btn-re-download {
          background: #252a38;
          color: #60a5fa;
          border: 1px solid rgba(96, 165, 250, 0.3);
          padding: 4px 8px;
          border-radius: 3px;
          font-size: 0.68rem;
          font-weight: 700;
          cursor: pointer;
        }
        .btn-re-download:hover {
          background: rgba(96, 165, 250, 0.2);
          color: #fff;
        }
        .history-empty-note {
          font-size: 0.78rem;
          color: #64748b;
          font-style: italic;
        }

        /* ==========================================================================
           PHASE 17: AI ILLUSTRATIONS, SUSPECT PHOTOS & LIGHTBOX STYLES
           ========================================================================== */

        .tab-btn-ai-illustrations.active {
          border-bottom-color: #a855f7 !important;
          color: #c084fc !important;
        }
        .ai-tab-sparkle {
          color: #c084fc;
        }

        /* Entity Portrait Cards (Victims & Suspects) */
        .entity-portrait-container {
          position: relative;
          width: 100%;
          height: 260px;
          overflow: hidden;
          background: #07090e;
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
          cursor: pointer;
        }
        .entity-card-portrait-img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          object-position: center top;
          transition: transform 0.25s ease;
          display: block;
        }
        .entity-portrait-container:hover .entity-card-portrait-img {
          transform: scale(1.03);
        }
        .entity-photo-badge {
          position: absolute;
          top: 8px;
          left: 8px;
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 3px 7px;
          border-radius: 3px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          letter-spacing: 0.08em;
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.5);
          backdrop-filter: blur(4px);
          z-index: 2;
        }
        .case-photo-badge {
          background: rgba(6, 78, 59, 0.92);
          border: 1px solid #10b981;
          color: #6ee7b7;
        }
        .synthetic-demo-label-overlay {
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          background: rgba(10, 14, 23, 0.92);
          border-top: 1px solid rgba(245, 158, 11, 0.45);
          padding: 6px 8px;
          text-align: center;
          backdrop-filter: blur(6px);
          z-index: 2;
        }
        .synthetic-label-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 800;
          color: #fef08a;
          letter-spacing: 0.06em;
          line-height: 1.2;
        }
        .synthetic-label-sub {
          font-family: var(--font-subheading, monospace);
          font-size: 0.58rem;
          font-weight: 600;
          color: #94a3b8;
          letter-spacing: 0.03em;
          margin-top: 1px;
        }
        .entity-card-quick-upload {
          padding: 8px 12px;
          background: rgba(15, 23, 42, 0.3);
          border-bottom: 1px solid rgba(255, 255, 255, 0.06);
        }
        .btn-upload-case-photo-bar {
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          padding: 6px 12px;
          background: rgba(245, 158, 11, 0.08);
          border: 1px dashed rgba(245, 158, 11, 0.4);
          color: #fbbf24;
          border-radius: 4px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.04em;
          cursor: pointer;
          transition: all 0.2s ease;
        }
        .btn-upload-case-photo-bar:hover {
          background: rgba(245, 158, 11, 0.2);
          border-style: solid;
          border-color: #f59e0b;
          color: #fef08a;
        }
        .suspect-card-actions {
          display: flex;
          align-items: center;
          gap: 6px;
        }
        .btn-photo-subtle {
          color: #fbbf24 !important;
          border-color: rgba(245, 158, 11, 0.3) !important;
        }
        .btn-photo-subtle:hover {
          background: rgba(245, 158, 11, 0.15) !important;
        }

        /* Evidence Card Image Views */
        .evidence-card-image-section {
          width: 100%;
          display: flex;
          flex-direction: column;
          gap: 8px;
          margin-bottom: 8px;
        }
        .evidence-photo-card-wrap {
          position: relative;
          width: 100%;
          max-width: 100%;
          height: 260px;
          overflow: hidden;
          background: #07090f;
          border-radius: 4px;
          border: 1px solid rgba(255, 255, 255, 0.08);
          cursor: pointer;
        }
        .evidence-img-container-inner {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;
          background: #080a0f;
        }
        .evidence-svg-fallback-wrap {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .evidence-svg-fallback-wrap svg {
          width: 100%;
          height: 100%;
          max-width: 100%;
          max-height: 260px;
          object-fit: contain;
          display: block;
        }
        .evidence-card-img {
          width: 100%;
          height: 100%;
          max-width: 100%;
          max-height: 260px;
          object-fit: contain;
          transition: transform 0.25s ease;
        }
        .evidence-photo-card-wrap:hover .evidence-card-img,
        .evidence-photo-card-wrap:hover .evidence-svg-fallback-wrap svg {
          transform: scale(1.02);
        }
        .badge-uploaded-evidence-banner {
          position: absolute;
          top: 8px;
          left: 8px;
          z-index: 5;
          display: inline-flex;
          align-items: center;
          gap: 5px;
          background: rgba(15, 23, 42, 0.94);
          border: 1px solid #06b6d4;
          border-radius: 3px;
          padding: 3px 8px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          letter-spacing: 0.08em;
          color: #22d3ee;
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.6);
          backdrop-filter: blur(4px);
        }
        .badge-ai-evidence-banner {
          position: absolute;
          top: 8px;
          left: 8px;
          right: 8px;
          z-index: 5;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 2px;
          background: rgba(15, 23, 42, 0.94);
          border: 1px solid #ef4444;
          border-radius: 3px;
          padding: 4px 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.6);
          backdrop-filter: blur(4px);
          text-align: center;
        }
        .badge-ai-line1 {
          display: flex;
          align-items: center;
          gap: 5px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          color: #f87171;
          letter-spacing: 0.08em;
        }
        .badge-ai-line2 {
          font-family: var(--font-subheading, monospace);
          font-size: 0.58rem;
          font-weight: 800;
          color: #fca5a5;
          letter-spacing: 0.06em;
        }

        .evidence-card-upload-action-row {
          display: flex;
          width: 100%;
        }
        .btn-upload-evidence-image {
          width: 100%;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          background: rgba(6, 182, 212, 0.12);
          border: 1px solid rgba(6, 182, 212, 0.45);
          color: #22d3ee;
          font-family: var(--font-subheading, monospace);
          font-size: 0.72rem;
          font-weight: 800;
          letter-spacing: 0.06em;
          padding: 8px 14px;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s ease;
        }
        .btn-upload-evidence-image:hover {
          background: rgba(6, 182, 212, 0.25);
          border-color: #06b6d4;
          color: #fff;
          box-shadow: 0 0 12px rgba(6, 182, 212, 0.35);
        }

        /* Replace Evidence Modal */
        .replace-evidence-modal {
          max-width: 540px;
          width: 95%;
        }
        .replace-notice-box {
          background: rgba(6, 182, 212, 0.08);
          border: 1px solid rgba(6, 182, 212, 0.25);
          border-radius: 4px;
          padding: 8px 12px;
        }
        .notice-box-header {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 4px;
        }
        .notice-box-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 800;
          color: #22d3ee;
          letter-spacing: 0.06em;
        }
        .notice-box-body {
          font-size: 0.72rem;
          color: #94a3b8;
          line-height: 1.4;
          margin: 0;
        }
        .btn-submit-upload-evidence {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          background: linear-gradient(180deg, #0891b2 0%, #0e7490 100%);
          border: 1px solid #06b6d4;
          color: #fff;
          font-family: var(--font-subheading, monospace);
          font-size: 0.72rem;
          font-weight: 800;
          letter-spacing: 0.06em;
          padding: 8px 16px;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.2s ease;
        }
        .btn-submit-upload-evidence:hover:not(:disabled) {
          background: linear-gradient(180deg, #06b6d4 0%, #0891b2 100%);
          box-shadow: 0 0 12px rgba(6, 182, 212, 0.4);
        }
        .btn-submit-upload-evidence:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        /* Evidence AI Modal */
        .evidence-ai-modal {
          max-width: 540px;
          width: 95%;
        }
        .evidence-ai-target-info {
          background: rgba(15, 23, 42, 0.6);
          border: 1px solid rgba(255, 255, 255, 0.08);
          border-radius: 4px;
          padding: 8px 12px;
          font-size: 0.75rem;
        }
        .target-info-item .lbl {
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          color: #94a3b8;
          margin-right: 6px;
        }
        .target-info-item .val {
          font-weight: 700;
          color: #f1f5f9;
          margin-right: 6px;
        }
        .target-info-item .mono-num {
          font-family: var(--font-mono, monospace);
          color: #c084fc;
        }

        /* Evidence Upload Preview Box */
        .evidence-upload-preview-box {
          background: #0f131d;
          border: 1px solid #06b6d4;
          border-radius: 4px;
          padding: 10px;
          margin-top: 12px;
        }
        .upload-preview-header {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 8px;
        }
        .upload-preview-badge-text {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 800;
          color: #22d3ee;
          letter-spacing: 0.08em;
        }
        .upload-preview-img-container {
          width: 100%;
          max-height: 200px;
          overflow: hidden;
          border-radius: 3px;
          background: #080a0f;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .upload-preview-img {
          max-width: 100%;
          max-height: 200px;
          object-fit: contain;
        }
        .upload-preview-footer {
          display: flex;
          justify-content: space-between;
          font-size: 0.7rem;
          color: #94a3b8;
          margin-top: 6px;
        }

        /* Suspect Photo Modal */
        .suspect-photo-modal {
          max-width: 520px;
          width: 95%;
        }
        .suspect-photo-modal-header-info {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 8px 12px;
          background: rgba(15, 23, 42, 0.6);
          border: 1px solid rgba(255, 255, 255, 0.08);
          border-radius: 4px;
          margin-bottom: 12px;
        }
        .suspect-modal-meta-row {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.78rem;
        }
        .suspect-modal-meta-row .meta-val {
          font-weight: 700;
          color: #f1f5f9;
        }
        .suspect-modal-meta-row .meta-sub {
          color: #fbbf24;
        }
        .suspect-photo-disclaimer-card {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          background: rgba(245, 158, 11, 0.08);
          border: 1px solid rgba(245, 158, 11, 0.25);
          border-radius: 4px;
          padding: 8px 10px;
          margin-bottom: 12px;
          font-size: 0.72rem;
          color: #fde68a;
          line-height: 1.4;
        }
        .suspect-modal-preview-box {
          background: #0f131d;
          border: 1px solid #f59e0b;
          border-radius: 4px;
          padding: 10px;
          margin-top: 12px;
        }
        .suspect-preview-badge-row {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 8px;
        }
        .suspect-preview-badge-text {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 800;
          color: #fbbf24;
          letter-spacing: 0.08em;
        }
        .suspect-preview-img-container {
          width: 100%;
          max-height: 220px;
          overflow: hidden;
          border-radius: 3px;
          background: #080a0f;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .suspect-preview-img {
          max-width: 100%;
          max-height: 220px;
          object-fit: contain;
        }
        .btn-save-amber {
          background: linear-gradient(180deg, #d97706 0%, #b45309 100%) !important;
          border-color: #f59e0b !important;
        }
        .btn-save-amber:hover {
          background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%) !important;
        }

        /* AI ILLUSTRATIONS TAB STYLES */
        .illustrations-tab-container {
          width: 100%;
        }
        .illustration-hero-panel {
          padding: 20px;
          background: linear-gradient(180deg, #131722 0%, #0d1017 100%);
          border-color: rgba(168, 85, 247, 0.25);
        }
        .illustration-hero-header {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 16px;
          margin-bottom: 16px;
        }
        .illustration-hero-title-group {
          display: flex;
          align-items: flex-start;
          gap: 12px;
        }
        .illustration-title-icon-box {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 40px;
          height: 40px;
          background: rgba(168, 85, 247, 0.15);
          border: 1px solid rgba(168, 85, 247, 0.35);
          border-radius: 6px;
          flex-shrink: 0;
        }
        .illustration-header-badge-row {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }
        .illustration-section-title {
          font-family: var(--font-subheading, monospace);
          font-size: 1rem;
          font-weight: 800;
          letter-spacing: 0.06em;
          color: #f1f5f9;
          margin: 0;
        }
        .badge-ai-model-tag {
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          color: #c084fc;
          background: rgba(168, 85, 247, 0.15);
          border: 1px solid rgba(168, 85, 247, 0.4);
          padding: 2px 8px;
          border-radius: 3px;
          letter-spacing: 0.08em;
        }
        .illustration-section-subtitle {
          font-size: 0.78rem;
          color: #94a3b8;
          margin-top: 4px;
          line-height: 1.4;
        }

        /* Mandatory Evidentiary Warning Box */
        .ai-evidentiary-warning-box {
          background: rgba(239, 68, 68, 0.08);
          border: 1px solid rgba(239, 68, 68, 0.35);
          border-radius: 4px;
          padding: 12px 14px;
          margin-bottom: 18px;
          border-left: 4px solid #ef4444;
        }
        .warning-box-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 6px;
        }
        .warning-box-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.72rem;
          font-weight: 800;
          color: #f87171;
          letter-spacing: 0.08em;
        }
        .warning-box-body {
          font-size: 0.74rem;
          color: #fca5a5;
          line-height: 1.45;
          margin: 0;
        }

        /* Prompt Builder Form */
        .illustration-form-builder {
          display: flex;
          flex-direction: column;
          gap: 14px;
        }
        .prompt-label-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 6px;
        }
        .prompt-hint {
          font-size: 0.7rem;
          color: #64748b;
        }
        .prompt-textarea {
          font-family: inherit;
          font-size: 0.82rem;
          line-height: 1.45;
          border-color: rgba(168, 85, 247, 0.3);
          background: #090c13;
        }
        .prompt-textarea:focus {
          border-color: #a855f7;
          box-shadow: 0 0 0 2px rgba(168, 85, 247, 0.2);
        }

        /* Preset Chips */
        .prompt-suggestions-container {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .suggestions-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          letter-spacing: 0.08em;
          color: #818cf8;
        }
        .suggestions-chips-grid {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .prompt-preset-chip {
          display: flex;
          align-items: center;
          gap: 8px;
          background: rgba(129, 140, 248, 0.06);
          border: 1px solid rgba(129, 140, 248, 0.2);
          border-radius: 4px;
          padding: 6px 10px;
          color: #c7d2fe;
          font-size: 0.73rem;
          text-align: left;
          cursor: pointer;
          transition: all 0.2s;
        }
        .prompt-preset-chip:hover:not(:disabled) {
          background: rgba(129, 140, 248, 0.15);
          border-color: #818cf8;
          color: #fff;
        }
        .prompt-preset-chip:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
        .preset-text {
          flex: 1;
        }

        /* Controls Row */
        .illustration-controls-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          flex-wrap: wrap;
          padding-top: 8px;
          border-top: 1px solid rgba(255, 255, 255, 0.08);
        }
        .aspect-ratio-selector-group {
          display: flex;
          align-items: center;
          gap: 10px;
        }
        .aspect-ratio-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 800;
          color: #8c93a8;
          letter-spacing: 0.08em;
        }
        .aspect-ratio-buttons {
          display: flex;
          gap: 6px;
        }
        .ar-btn {
          display: flex;
          flex-direction: column;
          align-items: center;
          background: #121622;
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 4px;
          padding: 4px 10px;
          cursor: pointer;
          transition: all 0.2s;
        }
        .ar-btn:hover:not(:disabled) {
          border-color: #a855f7;
        }
        .ar-btn.active {
          background: rgba(168, 85, 247, 0.15);
          border-color: #a855f7;
        }
        .ar-main-text {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 800;
          color: #f1f5f9;
        }
        .ar-btn.active .ar-main-text {
          color: #c084fc;
        }
        .ar-sub-text {
          font-size: 0.6rem;
          color: #64748b;
        }
        .btn-generate-ai-illustration {
          display: flex;
          align-items: center;
          gap: 8px;
          background: linear-gradient(180deg, #9333ea 0%, #7e22ce 100%);
          border: 1px solid #a855f7;
          border-radius: 4px;
          padding: 8px 18px;
          color: white;
          font-family: var(--font-subheading, monospace);
          font-size: 0.75rem;
          font-weight: 800;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all 0.2s;
          box-shadow: 0 2px 10px rgba(147, 51, 234, 0.35);
        }
        .btn-generate-ai-illustration:hover:not(:disabled) {
          background: linear-gradient(180deg, #a855f7 0%, #9333ea 100%);
          transform: translateY(-1px);
          box-shadow: 0 4px 14px rgba(147, 51, 234, 0.5);
        }
        .btn-generate-ai-illustration:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        /* Gallery Section */
        .illustrations-gallery-section {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }
        .gallery-header-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 4px 2px;
        }
        .gallery-header-left {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .gallery-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.78rem;
          font-weight: 800;
          color: #e2e8f0;
          letter-spacing: 0.08em;
        }
        .gallery-count-badge {
          background: #1e2433;
          color: #c084fc;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          padding: 2px 6px;
          border-radius: 3px;
        }
        .gallery-disclaimer-tag {
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          color: #ef4444;
          letter-spacing: 0.08em;
        }

        /* Gallery Grid */
        .illustrations-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
          gap: 20px;
        }
        .illustration-card {
          display: flex;
          flex-direction: column;
          overflow: hidden;
          border-color: rgba(168, 85, 247, 0.25);
        }
        .illustration-img-container {
          position: relative;
          width: 100%;
          height: 220px;
          overflow: hidden;
          background: #080a0f;
          cursor: pointer;
        }
        .illustration-card-img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.25s ease;
        }
        .illustration-img-container:hover .illustration-card-img {
          transform: scale(1.03);
        }
        .badge-ai-illustration-overlay {
          position: absolute;
          bottom: 8px;
          left: 8px;
          right: 8px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 5px;
          background: rgba(15, 23, 42, 0.94);
          border: 1px solid #ef4444;
          border-radius: 3px;
          padding: 4px 8px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.62rem;
          font-weight: 800;
          letter-spacing: 0.08em;
          color: #f87171;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.6);
          backdrop-filter: blur(4px);
          text-align: center;
        }
        .badge-ai-source-tag {
          position: absolute;
          top: 8px;
          right: 8px;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          background: rgba(88, 28, 135, 0.9);
          border: 1px solid #a855f7;
          border-radius: 3px;
          padding: 2px 6px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.6rem;
          font-weight: 800;
          color: #e9d5ff;
        }
        .illustration-card-body {
          padding: 12px;
          display: flex;
          flex-direction: column;
          gap: 10px;
          flex: 1;
        }
        .illustration-prompt-box {
          background: #0d1017;
          border: 1px solid rgba(255, 255, 255, 0.06);
          border-radius: 4px;
          padding: 8px 10px;
        }
        .prompt-quote-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.6rem;
          font-weight: 800;
          color: #a855f7;
          letter-spacing: 0.08em;
          display: block;
          margin-bottom: 2px;
        }
        .illustration-prompt-text {
          font-size: 0.74rem;
          color: #cbd5e1;
          line-height: 1.4;
          margin: 0;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .illustration-meta-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 6px;
          font-size: 0.7rem;
        }
        .ill-meta-item {
          display: flex;
          gap: 4px;
        }
        .ill-meta-item .meta-lbl {
          color: #64748b;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 700;
        }
        .ill-meta-item .meta-val {
          color: #94a3b8;
        }
        .illustration-card-footer {
          display: flex;
          align-items: center;
          gap: 6px;
          padding: 8px 12px;
          background: #0a0d14;
          border-top: 1px solid rgba(255, 255, 255, 0.06);
        }
        .btn-ill-action {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 8px;
          border-radius: 3px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          letter-spacing: 0.06em;
          cursor: pointer;
          text-decoration: none;
          transition: all 0.2s;
        }
        .btn-ill-action.view {
          background: #1e2433;
          color: #c084fc;
          border: 1px solid rgba(168, 85, 247, 0.3);
          flex: 1;
          justify-content: center;
        }
        .btn-ill-action.view:hover {
          background: rgba(168, 85, 247, 0.2);
          color: #fff;
        }
        .btn-ill-action.download {
          background: #172033;
          color: #60a5fa;
          border: 1px solid rgba(96, 165, 250, 0.3);
        }
        .btn-ill-action.download:hover {
          background: rgba(96, 165, 250, 0.2);
          color: #fff;
        }
        .btn-ill-action.delete {
          background: #2a1518;
          color: #f87171;
          border: 1px solid rgba(239, 68, 68, 0.3);
        }
        .btn-ill-action.delete:hover {
          background: rgba(239, 68, 68, 0.25);
          color: #fff;
        }

        /* LIGHTBOX MODAL */
        .lightbox-backdrop {
          z-index: 2000;
          background: rgba(5, 7, 12, 0.88);
          backdrop-filter: blur(8px);
        }
        .lightbox-modal {
          max-width: 90vw;
          max-height: 90vh;
          width: auto;
          display: flex;
          flex-direction: column;
          background: #0c0f17;
          border: 1px solid rgba(255, 255, 255, 0.15);
          border-radius: 6px;
          overflow: hidden;
          box-shadow: 0 10px 40px rgba(0, 0, 0, 0.8);
        }
        .lightbox-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 10px 16px;
          background: #101420;
          border-bottom: 1px solid rgba(255, 255, 255, 0.08);
          gap: 16px;
        }
        .lightbox-title-group {
          display: flex;
          align-items: center;
          gap: 10px;
          overflow: hidden;
        }
        .lightbox-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.82rem;
          font-weight: 700;
          color: #f1f5f9;
          margin: 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .badge-ai-lightbox {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          background: rgba(239, 68, 68, 0.15);
          border: 1px solid #ef4444;
          border-radius: 3px;
          padding: 2px 8px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          color: #f87171;
          white-space: nowrap;
        }
        .badge-photo-lightbox {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          background: rgba(245, 158, 11, 0.15);
          border: 1px solid #f59e0b;
          border-radius: 3px;
          padding: 2px 8px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          color: #fbbf24;
          white-space: nowrap;
        }
        .badge-evidence-lightbox {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          background: rgba(6, 182, 212, 0.15);
          border: 1px solid #06b6d4;
          border-radius: 3px;
          padding: 2px 8px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 800;
          color: #22d3ee;
          white-space: nowrap;
        }
        .lightbox-header-actions {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .btn-lightbox-action {
          color: #94a3b8;
          padding: 4px;
          display: flex;
          align-items: center;
          text-decoration: none;
          transition: color 0.2s;
        }
        .btn-lightbox-action:hover {
          color: #fff;
        }
        .btn-lightbox-close {
          background: transparent;
          border: none;
          color: #94a3b8;
          cursor: pointer;
          display: flex;
          align-items: center;
          padding: 4px;
          transition: color 0.2s;
        }
        .btn-lightbox-close:hover {
          color: #ef4444;
        }
        .lightbox-image-viewport {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 16px;
          background: #05070a;
          overflow: auto;
          max-height: 70vh;
        }
        .lightbox-full-img {
          max-width: 100%;
          max-height: 65vh;
          object-fit: contain;
          border-radius: 4px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.6);
        }
        .lightbox-footer {
          padding: 10px 16px;
          background: #101420;
          border-top: 1px solid rgba(255, 255, 255, 0.08);
        }
        .lightbox-subtitle-text {
          font-size: 0.72rem;
          color: #94a3b8;
          margin: 0;
          line-height: 1.4;
        }
      `}</style>
    </div>
  );
}
