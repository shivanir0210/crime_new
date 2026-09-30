import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import StatusBadge from '../components/StatusBadge';
import { 
  getCases, 
  getCaseById, 
  getCaseLocationIntelligence, 
  createCaseLocation, 
  deleteCaseLocation 
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import { 
  MapPin, 
  Search, 
  Filter, 
  ArrowRight, 
  Clock, 
  Shield, 
  Calendar, 
  Navigation, 
  Route as RouteIcon, 
  Layers, 
  AlertCircle, 
  Plus, 
  X, 
  RefreshCw, 
  ChevronRight, 
  Eye, 
  UserMinus, 
  UserX, 
  FileText, 
  Compass, 
  Maximize2, 
  CheckCircle2, 
  HelpCircle,
  ExternalLink,
  Trash2,
  Map as MapIcon
} from 'lucide-react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default icon path issues in Vite
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

export default function LocationIntelligencePage() {
  const { caseId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { officer } = useAuth();

  // -------------------------------------------------------------------------
  // STATE FOR SCREEN 1: CASE SELECTION
  // -------------------------------------------------------------------------
  const [cases, setCases] = useState([]);
  const [casesLoading, setCasesLoading] = useState(true);
  const [caseSearch, setCaseSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');

  // -------------------------------------------------------------------------
  // STATE FOR SCREEN 2: LOCATION MAP DASHBOARD
  // -------------------------------------------------------------------------
  const [caseData, setCaseData] = useState(null);
  const [intelSummary, setIntelSummary] = useState(null);
  const [mapLoading, setMapLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedEventId, setSelectedEventId] = useState(null);
  const [activeTab, setActiveTab] = useState('MAP'); // 'MAP' | 'TIMELINE' | 'TABLE'

  // Modal State for adding new location
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [addForm, setAddForm] = useState({
    entityType: 'VICTIM',
    entityName: '',
    latitude: '',
    longitude: '',
    locationName: '',
    address: '',
    recordedAt: new Date().toISOString().slice(0, 16),
    source: 'Authorized Location Data',
    locationStatus: 'HISTORICAL',
    verificationStatus: 'VERIFIED',
    notes: '',
    sequenceOrder: 1
  });
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  // Map reference and layers ref
  const mapContainerRef = useRef(null);
  const leafletMapRef = useRef(null);
  const markersLayerRef = useRef(null);
  const routeLayerRef = useRef(null);
  const markersRef = useRef({});
  const polylineRef = useRef(null);

  // -------------------------------------------------------------------------
  // LOAD CASES FOR SCREEN 1
  // -------------------------------------------------------------------------
  const fetchAccessibleCases = useCallback(async () => {
    try {
      setCasesLoading(true);
      setError(null);
      const res = await getCases({
        search: caseSearch.trim(),
        status: statusFilter !== 'ALL' ? statusFilter : '',
        priority: priorityFilter !== 'ALL' ? priorityFilter : '',
        page: 0,
        size: 50,
        sort: 'updatedAt,desc'
      });
      if (res && res.content) {
        setCases(res.content);
      } else if (Array.isArray(res)) {
        setCases(res);
      } else {
        setCases([]);
      }
    } catch (err) {
      console.error('Failed to load cases for Location Intelligence:', err);
      setError(err.message || 'Failed to load authorized cases.');
    } finally {
      setCasesLoading(false);
    }
  }, [caseSearch, statusFilter, priorityFilter]);

  useEffect(() => {
    if (!caseId) {
      fetchAccessibleCases();
    }
  }, [caseId, fetchAccessibleCases]);

  // -------------------------------------------------------------------------
  // LOAD CASE LOCATION INTELLIGENCE FOR SCREEN 2
  // -------------------------------------------------------------------------
  const fetchLocationData = useCallback(async () => {
    if (!caseId) return;
    try {
      setMapLoading(true);
      setError(null);

      // Fetch case details & location intelligence summary in parallel
      const [caseDetails, locationData] = await Promise.all([
        getCaseById(caseId).catch(() => null),
        getCaseLocationIntelligence(caseId)
      ]);

      setCaseData(caseDetails);
      setIntelSummary(locationData);
    } catch (err) {
      console.error('Failed to load location intelligence for case:', err);
      setError(err.message || 'Case not found or access restricted to your station.');
    } finally {
      setMapLoading(false);
    }
  }, [caseId]);

  useEffect(() => {
    if (caseId) {
      fetchLocationData();
    }
  }, [caseId, fetchLocationData]);

  // -------------------------------------------------------------------------
  // LEAFLET MAP INITIALIZATION & UPDATES
  // -------------------------------------------------------------------------
  const getMarkerColor = (entityType) => {
    switch ((entityType || '').toUpperCase()) {
      case 'VICTIM': return '#06B6D4'; // Cyan
      case 'CRIME_SCENE': return '#C1121F'; // Bright Red
      case 'LAST_KNOWN': return '#F59E0B'; // Amber
      case 'SUSPECT': return '#6366F1'; // Indigo
      case 'WITNESS': return '#10B981'; // Emerald
      case 'EVIDENCE': return '#A855F7'; // Purple
      default: return '#94A3B8'; // Slate
    }
  };

  const getStatusBadgeHtml = (status) => {
    const st = (status || 'HISTORICAL').toUpperCase();
    let bg = 'rgba(148, 163, 184, 0.15)';
    let color = '#94A3B8';
    let label = st;

    if (st === 'LIVE') {
      bg = 'rgba(16, 185, 129, 0.2)';
      color = '#10B981';
      label = '🟢 LIVE';
    } else if (st === 'LAST_KNOWN') {
      bg = 'rgba(245, 158, 11, 0.2)';
      color = '#F59E0B';
      label = '🟠 LAST KNOWN';
    } else if (st === 'ESTIMATED') {
      bg = 'rgba(168, 85, 247, 0.2)';
      color = '#A855F7';
      label = '🟣 ESTIMATED';
    } else {
      bg = 'rgba(148, 163, 184, 0.2)';
      color = '#CBD5E1';
      label = '⚪ HISTORICAL';
    }

    return `<span style="display:inline-block; font-size:0.68rem; font-weight:700; padding:2px 6px; border-radius:3px; background:${bg}; color:${color}; border:1px solid ${color}40;">${label}</span>`;
  };

  const createCustomIcon = (loc, index) => {
    const color = getMarkerColor(loc.entityType);
    const isCrimeScene = (loc.entityType || '').toUpperCase() === 'CRIME_SCENE';
    const isLive = (loc.locationStatus || '').toUpperCase() === 'LIVE';

    const pulseRing = (isCrimeScene || isLive)
      ? `<div style="position:absolute; top:-6px; left:-6px; width:36px; height:36px; border-radius:50%; background:${color}33; border:1px solid ${color}; animation: pulseRing 1.8s infinite ease-out;"></div>`
      : '';

    const seqLabel = loc.sequenceOrder || index + 1;

    const html = `
      <div style="position:relative; width:24px; height:24px; display:flex; align-items:center; justify-content:center; cursor:pointer;">
        ${pulseRing}
        <div style="width:24px; height:24px; border-radius:50%; background:${color}; border:2px solid #ffffff; box-shadow:0 0 12px ${color}, 0 2px 6px rgba(0,0,0,0.8); display:flex; align-items:center; justify-content:center; color:#000; font-family:var(--font-mono, monospace); font-size:11px; font-weight:800; z-index:2;">
          ${seqLabel}
        </div>
        <div style="position:absolute; bottom:-4px; left:50%; transform:translateX(-50%); width:0; height:0; border-left:4px solid transparent; border-right:4px solid transparent; border-top:6px solid ${color};"></div>
      </div>
    `;

    return L.divIcon({
      html: html,
      className: 'custom-crime-marker',
      iconSize: [24, 28],
      iconAnchor: [12, 28],
      popupAnchor: [0, -28]
    });
  };

  // 1. Initialize Leaflet map instance ONCE when container mounts
  useEffect(() => {
    if (!mapContainerRef.current || leafletMapRef.current) return;

    if (mapContainerRef.current._leaflet_id) {
      delete mapContainerRef.current._leaflet_id;
    }

    const map = L.map(mapContainerRef.current, {
      center: [11.0168, 76.9558],
      zoom: 12,
      zoomControl: true,
      attributionControl: true
    });

    leafletMapRef.current = map;

    // Standard OpenStreetMap Tile Layer
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19
    }).addTo(map);

    // Layer groups for markers & route polylines
    markersLayerRef.current = L.layerGroup().addTo(map);
    routeLayerRef.current = L.layerGroup().addTo(map);

    // Global helper for popup action button
    window.__crimesphere_open_evidence = (cId, eId) => {
      if (eId) {
        navigate(`/cases/${cId}?tab=EVIDENCE&evidenceId=${eId}`);
      } else {
        navigate(`/cases/${cId}?tab=EVIDENCE`);
      }
    };

    return () => {
      delete window.__crimesphere_open_evidence;
      if (leafletMapRef.current) {
        try {
          leafletMapRef.current.stop();
          if (markersLayerRef.current) {
            markersLayerRef.current.clearLayers();
          }
          if (routeLayerRef.current) {
            routeLayerRef.current.clearLayers();
          }
          leafletMapRef.current.remove();
        } catch (e) {
          // Ignore unmount cleanup errors
        }
        leafletMapRef.current = null;
        markersLayerRef.current = null;
        routeLayerRef.current = null;
      }
    };
  }, [caseId, navigate]);

  // 2. Synchronize markers, route polylines, and bounds whenever data updates
  useEffect(() => {
    const map = leafletMapRef.current;
    const markersLayer = markersLayerRef.current;
    const routeLayer = routeLayerRef.current;

    if (!map || !markersLayer || !routeLayer) return;

    markersLayer.clearLayers();
    routeLayer.clearLayers();
    markersRef.current = {};
    polylineRef.current = null;

    if (!intelSummary || !intelSummary.locations) return;

    const validLocations = intelSummary.locations.filter(
      l => l && Number.isFinite(l.latitude) && Number.isFinite(l.longitude)
    );

    const latLngs = [];

    validLocations.forEach((loc, index) => {
      const latLng = [loc.latitude, loc.longitude];
      latLngs.push(latLng);

      const customIcon = createCustomIcon(loc, index);
      const marker = L.marker(latLng, { icon: customIcon });

      const statusBadge = getStatusBadgeHtml(loc.locationStatus);
      const entityColor = getMarkerColor(loc.entityType);

      const popupContent = `
        <div class="custom-map-popup">
          <div class="popup-header">
            <div style="display:flex; align-items:center; gap:6px;">
              <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:${entityColor};"></span>
              <span class="popup-entity-title">${loc.entityName || 'Location'}</span>
            </div>
            ${statusBadge}
          </div>
          <div class="popup-body">
            <div class="popup-row">
              <span class="popup-row-label">Category:</span>
              <span class="popup-row-val" style="color:${entityColor}; font-weight:700;">${loc.entityType || 'UNKNOWN'}</span>
            </div>
            <div class="popup-row">
              <span class="popup-row-label">Location:</span>
              <span class="popup-row-val">${loc.locationName || 'N/A'}</span>
            </div>
            ${loc.address ? `
              <div class="popup-row">
                <span class="popup-row-label">Address:</span>
                <span class="popup-row-val" style="font-size:0.75rem; color:#a7a7aa;">${loc.address}</span>
              </div>
            ` : ''}
            <div class="popup-row">
              <span class="popup-row-label">Coordinates:</span>
              <span class="popup-row-val" style="font-family:monospace; font-size:0.75rem; color:#38bdf8;">${loc.latitude.toFixed(4)}° N, ${loc.longitude.toFixed(4)}° E</span>
            </div>
            <div class="popup-row">
              <span class="popup-row-label">Source:</span>
              <span class="popup-row-val">${loc.source || 'Authorized Record'}</span>
            </div>
            <div class="popup-row">
              <span class="popup-row-label">Verification:</span>
              <span class="popup-row-val" style="color:${loc.verificationStatus === 'VERIFIED' ? '#10b981' : '#f59e0b'}; font-weight:600;">${loc.verificationStatus || 'VERIFIED'}</span>
            </div>
            ${loc.notes ? `
              <div class="popup-row" style="margin-top:4px; padding-top:4px; border-top:1px solid #2a2a2d;">
                <span class="popup-row-val" style="font-style:italic; font-size:0.75rem; color:#a7a7aa;">"${loc.notes}"</span>
              </div>
            ` : ''}
          </div>
          <div class="popup-footer">
            <button 
              class="popup-action-btn"
              onclick="window.__crimesphere_open_evidence && window.__crimesphere_open_evidence('${caseId}', '${loc.entityId || ''}')"
            >
              View Related Evidence ↗
            </button>
          </div>
        </div>
      `;

      marker.addTo(markersLayer);
      marker.bindPopup(popupContent, { maxWidth: 320 });
      marker.on('click', () => {
        setSelectedEventId(loc.id);
      });

      markersRef.current[loc.id] = marker;
    });

    // Draw Connected Investigation Route Polyline if 2+ points exist
    if (latLngs.length >= 2) {
      const routePolyline = L.polyline(latLngs, {
        color: '#C1121F',
        weight: 3.5,
        opacity: 0.85,
        dashArray: '8, 8',
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(routeLayer);

      routePolyline.bindTooltip(
        `<strong>Possible/Recorded Route</strong><br/>Distance: ${intelSummary.formattedDistance || 'N/A'}<br/>Est. Transit: ${intelSummary.formattedTravelTime || 'N/A'}`,
        { sticky: true, className: 'route-map-tooltip' }
      );

      polylineRef.current = routePolyline;
    }

    // Auto-fit map bounds
    const timer = setTimeout(() => {
      if (leafletMapRef.current) {
        leafletMapRef.current.invalidateSize();
        if (latLngs.length > 1) {
          leafletMapRef.current.fitBounds(latLngs, { padding: [40, 40], maxZoom: 14 });
        } else if (latLngs.length === 1) {
          leafletMapRef.current.setView(latLngs[0], 14);
        }
      }
    }, 60);

    return () => clearTimeout(timer);
  }, [intelSummary, caseId]);

  // 3. Handle active tab resize
  useEffect(() => {
    if (activeTab === 'MAP' && leafletMapRef.current) {
      const timer = setTimeout(() => {
        if (leafletMapRef.current) {
          leafletMapRef.current.invalidateSize();
        }
      }, 60);
      return () => clearTimeout(timer);
    }
  }, [activeTab]);

  // Focus on specific event/marker when clicked in right panel
  const handleEventClick = (event) => {
    if (!event) return;
    setSelectedEventId(event.locationId);
    const marker = markersRef.current[event.locationId];
    if (marker && leafletMapRef.current) {
      leafletMapRef.current.flyTo(marker.getLatLng(), 14, { duration: 0.8 });
      marker.openPopup();
    }
  };

  const handleResetView = () => {
    if (!leafletMapRef.current || !intelSummary) return;
    const validLocations = (intelSummary.locations || []).filter(
      l => l && Number.isFinite(l.latitude) && Number.isFinite(l.longitude)
    );
    const latLngs = validLocations.map(l => [l.latitude, l.longitude]);
    leafletMapRef.current.invalidateSize();
    if (latLngs.length > 1) {
      leafletMapRef.current.fitBounds(latLngs, { padding: [40, 40], maxZoom: 14 });
    } else if (latLngs.length === 1) {
      leafletMapRef.current.setView(latLngs[0], 14);
    }
  };

  // -------------------------------------------------------------------------
  // ADD NEW LOCATION POINT MODAL HANDLER
  // -------------------------------------------------------------------------
  const handleAddLocationSubmit = async (e) => {
    e.preventDefault();
    setFormError(null);

    const latNum = parseFloat(addForm.latitude);
    const lonNum = parseFloat(addForm.longitude);

    if (isNaN(latNum) || isNaN(lonNum)) {
      setFormError('Please enter valid numeric latitude and longitude coordinates.');
      return;
    }
    if (!addForm.locationName.trim()) {
      setFormError('Location name is required.');
      return;
    }
    if (!addForm.entityName.trim()) {
      setFormError('Entity or Person of Interest name is required.');
      return;
    }

    try {
      setFormSubmitting(true);
      await createCaseLocation(caseId, {
        ...addForm,
        latitude: latNum,
        longitude: lonNum,
        recordedAt: addForm.recordedAt ? new Date(addForm.recordedAt).toISOString() : new Date().toISOString()
      });

      setIsAddModalOpen(false);
      // Reset form
      setAddForm({
        entityType: 'VICTIM',
        entityName: '',
        latitude: '',
        longitude: '',
        locationName: '',
        address: '',
        recordedAt: new Date().toISOString().slice(0, 16),
        source: 'Authorized Location Data',
        locationStatus: 'HISTORICAL',
        verificationStatus: 'VERIFIED',
        notes: '',
        sequenceOrder: (intelSummary?.locations?.length || 0) + 1
      });

      // Reload fresh data
      await fetchLocationData();
    } catch (err) {
      console.error('Failed to add location point:', err);
      setFormError(err.message || 'Failed to record location point.');
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleDeleteLocation = async (locId) => {
    if (!window.confirm('Are you sure you want to remove this location intelligence entry?')) return;
    try {
      await deleteCaseLocation(caseId, locId);
      await fetchLocationData();
    } catch (err) {
      console.error('Failed to delete location:', err);
      alert('Failed to remove location record: ' + err.message);
    }
  };

  // =========================================================================
  // RENDER SCREEN 1: CASE SELECTION FOR INVESTIGATION
  // =========================================================================
  if (!caseId) {
    return (
      <div className="cs-dashboard-layout">
        <TopBar />
        <div className="cs-main-wrapper">
          <Sidebar />
          <main className="cs-content-area">
            <div className="location-intel-container">
              
              {/* Header Section */}
              <div className="intel-select-screen">
                <div className="intel-header-section">
                  <div className="intel-title-row">
                    <h1 className="intel-main-title">
                      <MapPin className="pin-icon" size={26} />
                      LOCATION INTELLIGENCE
                    </h1>
                    <div className="security-badge">
                      <Shield size={13} className="text-crimson" />
                      <span>POLICE GIS INVESTIGATION MODULE</span>
                    </div>
                  </div>
                  <p className="intel-subtitle">
                    Select a case to visualize locations, movement history, routes and crime-scene relationships.
                  </p>
                </div>

                {/* Search & Filter Controls */}
                <div className="intel-controls-bar">
                  <div className="intel-search-box">
                    <Search size={16} className="text-muted" />
                    <input 
                      type="text" 
                      className="intel-search-input"
                      placeholder="Search cases by number, title, or crime location..."
                      value={caseSearch}
                      onChange={(e) => setCaseSearch(e.target.value)}
                    />
                    {caseSearch && (
                      <button 
                        onClick={() => setCaseSearch('')}
                        style={{ background: 'none', border: 'none', color: '#6f6f73', cursor: 'pointer' }}
                      >
                        <X size={14} />
                      </button>
                    )}
                  </div>

                  <div className="intel-filter-group">
                    <select 
                      className="intel-filter-select"
                      value={statusFilter}
                      onChange={(e) => setStatusFilter(e.target.value)}
                    >
                      <option value="ALL">All Statuses</option>
                      <option value="OPEN">Open</option>
                      <option value="UNDER_INVESTIGATION">Under Investigation</option>
                      <option value="PENDING_REVIEW">Pending Review</option>
                      <option value="CLOSED">Closed</option>
                    </select>

                    <select 
                      className="intel-filter-select"
                      value={priorityFilter}
                      onChange={(e) => setPriorityFilter(e.target.value)}
                    >
                      <option value="ALL">All Priorities</option>
                      <option value="CRITICAL">Critical</option>
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>
                  </div>
                </div>

                {/* Error Banner */}
                {error && (
                  <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <AlertCircle size={18} />
                    <span>{error}</span>
                  </div>
                )}

                {/* Cases Grid */}
                {casesLoading ? (
                  <div className="intel-empty-state">
                    <RefreshCw className="empty-state-icon spin" size={32} />
                    <div className="empty-state-title">LOADING AUTHORIZED CASES...</div>
                    <div className="empty-state-text">Verifying station isolation and police credentials.</div>
                  </div>
                ) : cases.length === 0 ? (
                  <div className="intel-empty-state">
                    <Compass className="empty-state-icon" size={36} />
                    <div className="empty-state-title">NO ACCESSIBLE CASES FOUND</div>
                    <div className="empty-state-text">
                      No cases match your filter criteria or have been assigned to your station.
                    </div>
                  </div>
                ) : (
                  <div className="intel-cases-grid">
                    {cases.map((c) => {
                      const locationStr = c.incidentLocation || c.location || (c.title && c.title.includes('at') ? c.title.split('at')[1].trim() : 'Station Jurisdiction');
                      const incidentDateStr = c.incidentDate 
                        ? new Date(c.incidentDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
                        : (c.createdAt ? new Date(c.createdAt).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Recorded');

                      return (
                        <div key={c.id} className="intel-case-card">
                          <div>
                            <div className="intel-card-header">
                              <span className="intel-case-number">CASE #{c.caseNumber}</span>
                              <div className="intel-card-badges">
                                <StatusBadge status={c.status || 'OPEN'} />
                                <StatusBadge status={c.priority || 'MEDIUM'} />
                              </div>
                            </div>

                            <h3 className="intel-case-title">{c.title}</h3>
                          </div>

                          <div className="intel-card-meta">
                            <div className="intel-meta-item">
                              <MapPin size={14} className="meta-icon text-crimson" />
                              <span>Incident Location:</span>
                              <span className="meta-val">{locationStr}</span>
                            </div>
                            <div className="intel-meta-item">
                              <Calendar size={14} className="meta-icon" />
                              <span>Incident / Reg Date:</span>
                              <span className="meta-val">{incidentDateStr}</span>
                            </div>
                            <div className="intel-meta-item">
                              <Shield size={14} className="meta-icon text-muted" />
                              <span>Officer:</span>
                              <span className="meta-val">{c.createdBy?.fullName || 'Assigned Officer'}</span>
                            </div>
                          </div>

                          <div className="intel-card-actions">
                            <button 
                              className="btn-view-map"
                              onClick={() => navigate(`/location-intelligence/${c.id}`)}
                            >
                              <MapIcon size={14} />
                              View Location Map
                              <ArrowRight size={14} />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

            </div>
          </main>
        </div>
      </div>
    );
  }

  // =========================================================================
  // RENDER SCREEN 2: INTERACTIVE MAP & GIS DASHBOARD
  // =========================================================================
  const locations = intelSummary?.locations || [];
  const events = intelSummary?.events || [];
  const hasLocations = locations.length > 0;

  return (
    <div className="cs-dashboard-layout">
      <TopBar />
      <div className="cs-main-wrapper">
        <Sidebar />
        <main className="cs-content-area">
          <div className="location-intel-container">
            
            {/* Top Navigation & Case Banner */}
            <div className="intel-nav-bar">
              <div className="intel-nav-left">
                <div className="intel-brand-badge">
                  <MapPin size={15} />
                  LOCATION INTELLIGENCE
                </div>
                <div className="intel-current-case">
                  <span className="intel-case-tag">{intelSummary?.caseNumber || caseData?.caseNumber || 'CASE DOSSIER'}</span>
                  <span className="intel-case-heading">
                    {intelSummary?.caseTitle || caseData?.title || 'Investigation Map'}
                  </span>
                  {intelSummary?.caseStatus && (
                    <StatusBadge status={intelSummary.caseStatus} />
                  )}
                  {intelSummary?.casePriority && (
                    <StatusBadge status={intelSummary.casePriority} />
                  )}
                </div>
              </div>

              <div className="intel-nav-actions">
                <button 
                  className="btn-intel-secondary"
                  onClick={() => navigate('/location-intelligence')}
                  title="Switch to another case"
                >
                  <ChevronRight size={15} style={{ transform: 'rotate(180deg)' }} />
                  Change Case
                </button>

                <button 
                  className="btn-intel-secondary"
                  onClick={() => navigate(`/cases/${caseId}`)}
                  title="Open full case file"
                >
                  <ExternalLink size={14} />
                  Case Dossier
                </button>

                <button 
                  className="btn-intel-primary"
                  onClick={() => setIsAddModalOpen(true)}
                >
                  <Plus size={15} />
                  Add Location Point
                </button>
              </div>
            </div>

            {/* Error Message */}
            {error && (
              <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <AlertCircle size={18} />
                <span>{error}</span>
              </div>
            )}

            {/* Summary Cards Strip (Calculated dynamically from real case location data) */}
            <div className="intel-summary-strip">
              <div className="intel-stat-card">
                <div className="intel-stat-icon-wrap cyan">
                  <RouteIcon size={20} />
                </div>
                <div className="intel-stat-content">
                  <span className="intel-stat-label">DISTANCE</span>
                  <span className="intel-stat-value">{intelSummary?.formattedDistance || (hasLocations ? '0.0 km' : 'N/A')}</span>
                  <span className="intel-stat-sub">Geodesic Sequential Route</span>
                </div>
              </div>

              <div className="intel-stat-card">
                <div className="intel-stat-icon-wrap amber">
                  <Clock size={20} />
                </div>
                <div className="intel-stat-content">
                  <span className="intel-stat-label">TRAVEL TIME</span>
                  <span className="intel-stat-value">{intelSummary?.formattedTravelTime || (hasLocations ? '0m' : 'N/A')}</span>
                  <span className="intel-stat-sub">Estimated Road Transit</span>
                </div>
              </div>

              <div className="intel-stat-card">
                <div className="intel-stat-icon-wrap red">
                  <MapPin size={20} />
                </div>
                <div className="intel-stat-content">
                  <span className="intel-stat-label">KNOWN LOCATIONS</span>
                  <span className="intel-stat-value">{intelSummary?.totalLocations || 0}</span>
                  <span className="intel-stat-sub">Geo-referenced coordinates</span>
                </div>
              </div>

              <div className="intel-stat-card">
                <div className="intel-stat-icon-wrap purple">
                  <Navigation size={20} />
                </div>
                <div className="intel-stat-content">
                  <span className="intel-stat-label">LOCATION EVENTS</span>
                  <span className="intel-stat-value">{intelSummary?.totalEvents || 0}</span>
                  <span className="intel-stat-sub">
                    {intelSummary?.liveCount > 0 ? `${intelSummary.liveCount} LIVE Tracking` : 'Historical & Evidence'}
                  </span>
                </div>
              </div>
            </div>

            {/* Main Interactive Map & Events Workspace */}
            <div className="intel-workspace-grid">
              
              {/* Left Area: GIS Interactive Map */}
              <div className="intel-map-pane">
                <div className="intel-map-topbar">
                  <div className="intel-map-title">
                    <Compass size={16} className="text-crimson" />
                    CRIME SCENE GIS MAPPING DASHBOARD
                  </div>

                  <div className="intel-map-controls-group">
                    <button 
                      className="map-ctrl-btn"
                      onClick={handleResetView}
                      title="Fit all markers in view"
                      disabled={!hasLocations || mapLoading}
                    >
                      <Maximize2 size={13} />
                      Fit Bounds
                    </button>
                  </div>
                </div>

                <div className="intel-leaflet-wrapper">
                  <div ref={mapContainerRef} className="intel-leaflet-container" />

                  {/* Loading Overlay */}
                  {mapLoading && (
                    <div className="intel-map-loading-overlay">
                      <RefreshCw className="empty-state-icon spin" size={32} />
                      <div className="empty-state-title" style={{ color: '#fff', fontSize: '1rem', marginTop: '6px' }}>
                        CALIBRATING GIS INTELLIGENCE...
                      </div>
                      <div style={{ color: '#a7a7aa', fontSize: '0.8rem' }}>
                        Loading crime scene coordinates and route vectors
                      </div>
                    </div>
                  )}

                  {/* No Locations Empty Overlay */}
                  {!mapLoading && !hasLocations && (
                    <div className="intel-map-empty-overlay">
                      <Compass className="empty-state-icon" size={42} style={{ color: '#C1121F' }} />
                      <div className="empty-state-title" style={{ color: '#fff', fontSize: '1rem', fontWeight: 700 }}>
                        NO GEOGRAPHIC COORDINATES RECORDED
                      </div>
                      <p className="empty-state-text" style={{ maxWidth: '420px', fontSize: '0.82rem', color: '#a7a7aa', margin: '4px 0 12px 0' }}>
                        This case currently has no verified location coordinates. Add coordinates from FIR scene surveys, cellular CDR logs, or physical evidence recovery.
                      </p>
                      <button 
                        className="btn-intel-primary"
                        onClick={() => setIsAddModalOpen(true)}
                        style={{ padding: '7px 16px', fontSize: '0.85rem' }}
                      >
                        <Plus size={14} />
                        Add Location Evidence
                      </button>
                    </div>
                  )}

                    {/* Compact Map Legend */}
                    <div className="intel-map-legend">
                      <div className="legend-title">LOCATION LEGEND</div>
                      <div className="legend-grid">
                        <div className="legend-item">
                          <span className="legend-dot" style={{ background: '#06B6D4' }}></span>
                          <span>Victim Location</span>
                        </div>
                        <div className="legend-item">
                          <span className="legend-dot" style={{ background: '#C1121F' }}></span>
                          <span>Crime Scene</span>
                        </div>
                        <div className="legend-item">
                          <span className="legend-dot" style={{ background: '#F59E0B' }}></span>
                          <span>Last Known</span>
                        </div>
                        <div className="legend-item">
                          <span className="legend-dot" style={{ background: '#6366F1' }}></span>
                          <span>Suspect Location</span>
                        </div>
                        <div className="legend-item">
                          <span className="legend-dot" style={{ background: '#10B981' }}></span>
                          <span>Witness Location</span>
                        </div>
                        <div className="legend-item">
                          <span className="legend-dot" style={{ background: '#A855F7' }}></span>
                          <span>Evidence Location</span>
                        </div>
                      </div>

                      <div className="legend-status-row">
                        <span>🟢 LIVE</span>
                        <span>🟠 LAST KNOWN</span>
                        <span>⚪ HISTORICAL</span>
                        <span>🟣 ESTIMATED</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Right Area: Chronological Location Events Panel */}
                <div className="intel-events-pane">
                  <div className="intel-events-header">
                    <div className="events-title-wrap">
                      <Clock size={16} className="text-crimson" />
                      <span>LOCATION EVENTS</span>
                    </div>
                    <span className="events-count-badge">{events.length} LOGS</span>
                  </div>

                  <div className="intel-events-list">
                    {events.map((ev, idx) => {
                      const isSelected = selectedEventId === ev.locationId;
                      const entityTypeClass = (ev.entityType || 'other').toLowerCase();

                      return (
                        <div 
                          key={ev.id || idx}
                          className={`intel-event-card ${entityTypeClass} ${isSelected ? 'selected' : ''}`}
                          onClick={() => handleEventClick(ev)}
                        >
                          <div className="event-card-top">
                            <span className="event-time-badge">
                              <Clock size={12} className="text-muted" />
                              {ev.formattedTime || 'Recorded Time'}
                            </span>
                            <div style={{ display: 'flex', gap: '4px' }}>
                              <span style={{ 
                                fontSize: '0.68rem', 
                                fontWeight: 700, 
                                padding: '1px 5px', 
                                borderRadius: '2px', 
                                background: ev.locationStatus === 'LIVE' ? 'rgba(16,185,129,0.15)' : 'rgba(245,158,11,0.15)', 
                                color: ev.locationStatus === 'LIVE' ? '#10B981' : '#F59E0B'
                              }}>
                                {ev.locationStatus || 'HISTORICAL'}
                              </span>
                            </div>
                          </div>

                          <div className="event-entity-row">
                            <MapPin size={15} style={{ color: getMarkerColor(ev.entityType), flexShrink: 0 }} />
                            <span className="event-entity-name">{ev.entityName}</span>
                          </div>

                          <div className="event-location-text">
                            <Compass size={13} className="text-muted" style={{ flexShrink: 0, marginTop: '2px' }} />
                            <span>{ev.locationName} {ev.address ? `(${ev.address})` : ''}</span>
                          </div>

                          <div className="event-meta-footer">
                            <span className="event-source-tag">Source: {ev.source || 'Authorized Data'}</span>
                            <span style={{ 
                              color: ev.verificationStatus === 'VERIFIED' ? '#10B981' : '#F59E0B', 
                              fontWeight: 600 
                            }}>
                              {ev.verificationStatus || 'VERIFIED'}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

              </div>

            {/* Bottom Analysis & Breakdown Table */}
            {hasLocations && (
              <div style={{
                background: 'var(--bg-panel)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--panel-radius)',
                padding: '16px',
                display: 'flex',
                flexDirection: 'column',
                gap: '12px'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontFamily: 'var(--font-heading)', fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-bright)' }}>
                    <RouteIcon size={16} className="text-crimson" />
                    LOCATION RECONSTRUCTION & SEQUENCE BREAKDOWN
                  </div>
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                    {locations.length} points plotted along route sequence
                  </span>
                </div>

                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)', fontFamily: 'var(--font-subheading)', fontSize: '0.72rem', letterSpacing: '0.08em' }}>
                        <th style={{ padding: '8px 10px' }}>SEQ</th>
                        <th style={{ padding: '8px 10px' }}>ENTITY / SUBJECT</th>
                        <th style={{ padding: '8px 10px' }}>CATEGORY</th>
                        <th style={{ padding: '8px 10px' }}>LOCATION & VENUE</th>
                        <th style={{ padding: '8px 10px' }}>COORDINATES</th>
                        <th style={{ padding: '8px 10px' }}>STATUS</th>
                        <th style={{ padding: '8px 10px' }}>SOURCE</th>
                        <th style={{ padding: '8px 10px', textAlign: 'right' }}>ACTIONS</th>
                      </tr>
                    </thead>
                    <tbody>
                      {locations.map((loc, idx) => (
                        <tr 
                          key={loc.id} 
                          style={{ 
                            borderBottom: '1px solid rgba(42,42,45,0.4)',
                            transition: 'background 0.15s ease'
                          }}
                          className="table-row-hover"
                        >
                          <td style={{ padding: '10px', fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--bright-crime-red)' }}>
                            #{loc.sequenceOrder || idx + 1}
                          </td>
                          <td style={{ padding: '10px', fontWeight: 600, color: 'var(--text-bright)' }}>
                            {loc.entityName}
                          </td>
                          <td style={{ padding: '10px' }}>
                            <span style={{ 
                              color: getMarkerColor(loc.entityType), 
                              fontWeight: 700, 
                              fontSize: '0.75rem' 
                            }}>
                              {loc.entityType}
                            </span>
                          </td>
                          <td style={{ padding: '10px', color: 'var(--text-primary)' }}>
                            {loc.locationName}
                          </td>
                          <td style={{ padding: '10px', fontFamily: 'var(--font-mono)', fontSize: '0.75rem', color: '#38bdf8' }}>
                            {loc.latitude?.toFixed(4)}° N, {loc.longitude?.toFixed(4)}° E
                          </td>
                          <td style={{ padding: '10px' }}>
                            <span style={{ 
                              fontSize: '0.7rem', 
                              fontWeight: 700, 
                              padding: '2px 6px', 
                              borderRadius: '3px',
                              background: loc.locationStatus === 'LIVE' ? 'rgba(16,185,129,0.15)' : 'rgba(245,158,11,0.15)',
                              color: loc.locationStatus === 'LIVE' ? '#10B981' : '#F59E0B'
                            }}>
                              {loc.locationStatus || 'HISTORICAL'}
                            </span>
                          </td>
                          <td style={{ padding: '10px', color: 'var(--text-secondary)', fontSize: '0.78rem' }}>
                            {loc.source}
                          </td>
                          <td style={{ padding: '10px', textAlign: 'right' }}>
                            <button 
                              onClick={() => handleDeleteLocation(loc.id)}
                              style={{ 
                                background: 'none', 
                                border: 'none', 
                                color: '#ef4444', 
                                cursor: 'pointer',
                                padding: '4px',
                                borderRadius: '3px'
                              }}
                              title="Delete location point"
                            >
                              <Trash2 size={15} />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

          </div>
        </main>
      </div>

      {/* =====================================================================
          MODAL: ADD NEW LOCATION POINT
          ===================================================================== */}
      {isAddModalOpen && (
        <div className="intel-modal-overlay" onClick={() => setIsAddModalOpen(false)}>
          <div className="intel-modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="intel-modal-header">
              <div className="modal-header-title">
                <MapPin size={18} className="text-crimson" />
                ADD LOCATION INTELLIGENCE POINT
              </div>
              <button className="modal-close-btn" onClick={() => setIsAddModalOpen(false)}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleAddLocationSubmit}>
              <div className="intel-modal-body">
                {formError && (
                  <div className="alert alert-danger" style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.82rem' }}>
                    <AlertCircle size={15} />
                    <span>{formError}</span>
                  </div>
                )}

                <div className="intel-form-grid">
                  <div className="intel-form-group">
                    <label className="intel-form-label">ENTITY / SUBJECT TYPE *</label>
                    <select 
                      className="intel-form-select"
                      value={addForm.entityType}
                      onChange={(e) => setAddForm({ ...addForm, entityType: e.target.value })}
                      required
                    >
                      <option value="VICTIM">Victim Location</option>
                      <option value="CRIME_SCENE">Crime Scene</option>
                      <option value="LAST_KNOWN">Last Known Location</option>
                      <option value="SUSPECT">Suspect Location</option>
                      <option value="WITNESS">Witness Location</option>
                      <option value="EVIDENCE">Evidence Location</option>
                      <option value="OTHER">Other Intelligence Point</option>
                    </select>
                  </div>

                  <div className="intel-form-group">
                    <label className="intel-form-label">SUBJECT / PERSON NAME *</label>
                    <input 
                      type="text" 
                      className="intel-form-input"
                      placeholder="e.g. K. Senthil Kumar or Primary Scene"
                      value={addForm.entityName}
                      onChange={(e) => setAddForm({ ...addForm, entityName: e.target.value })}
                      required
                    />
                  </div>

                  <div className="intel-form-group">
                    <label className="intel-form-label">LATITUDE (DECIMAL DEG) *</label>
                    <input 
                      type="number" 
                      step="any"
                      className="intel-form-input"
                      placeholder="e.g. 11.0168"
                      value={addForm.latitude}
                      onChange={(e) => setAddForm({ ...addForm, latitude: e.target.value })}
                      required
                    />
                  </div>

                  <div className="intel-form-group">
                    <label className="intel-form-label">LONGITUDE (DECIMAL DEG) *</label>
                    <input 
                      type="number" 
                      step="any"
                      className="intel-form-input"
                      placeholder="e.g. 76.9558"
                      value={addForm.longitude}
                      onChange={(e) => setAddForm({ ...addForm, longitude: e.target.value })}
                      required
                    />
                  </div>

                  <div className="intel-form-group full-width">
                    <label className="intel-form-label">LOCATION / VENUE NAME *</label>
                    <input 
                      type="text" 
                      className="intel-form-input"
                      placeholder="e.g. Gandhipuram Cross Cut Road or Pollachi Tollway"
                      value={addForm.locationName}
                      onChange={(e) => setAddForm({ ...addForm, locationName: e.target.value })}
                      required
                    />
                  </div>

                  <div className="intel-form-group full-width">
                    <label className="intel-form-label">PHYSICAL ADDRESS</label>
                    <input 
                      type="text" 
                      className="intel-form-input"
                      placeholder="e.g. No 45, Cross Cut Road, Gandhipuram, Coimbatore"
                      value={addForm.address}
                      onChange={(e) => setAddForm({ ...addForm, address: e.target.value })}
                    />
                  </div>

                  <div className="intel-form-group">
                    <label className="intel-form-label">RECORDED TIMESTAMP</label>
                    <input 
                      type="datetime-local" 
                      className="intel-form-input"
                      value={addForm.recordedAt}
                      onChange={(e) => setAddForm({ ...addForm, recordedAt: e.target.value })}
                    />
                  </div>

                  <div className="intel-form-group">
                    <label className="intel-form-label">SOURCE OF LOCATION DATA *</label>
                    <input 
                      type="text" 
                      className="intel-form-input"
                      placeholder="e.g. Authorized CDR / FIR / CCTV Footage"
                      value={addForm.source}
                      onChange={(e) => setAddForm({ ...addForm, source: e.target.value })}
                      required
                    />
                  </div>

                  <div className="intel-form-group">
                    <label className="intel-form-label">TRACKING STATUS</label>
                    <select 
                      className="intel-form-select"
                      value={addForm.locationStatus}
                      onChange={(e) => setAddForm({ ...addForm, locationStatus: e.target.value })}
                    >
                      <option value="HISTORICAL">⚪ HISTORICAL (Recorded prior event)</option>
                      <option value="LAST_KNOWN">🟠 LAST KNOWN (Most recent confirmed)</option>
                      <option value="LIVE">🟢 LIVE (Active authorized tracking)</option>
                      <option value="ESTIMATED">🟣 ESTIMATED (Derived / Inferred)</option>
                    </select>
                  </div>

                  <div className="intel-form-group">
                    <label className="intel-form-label">SEQUENCE ORDER</label>
                    <input 
                      type="number" 
                      min="1"
                      className="intel-form-input"
                      value={addForm.sequenceOrder}
                      onChange={(e) => setAddForm({ ...addForm, sequenceOrder: parseInt(e.target.value) || 1 })}
                    />
                  </div>

                  <div className="intel-form-group full-width">
                    <label className="intel-form-label">INTELLIGENCE NOTES / CONTEXT</label>
                    <textarea 
                      className="intel-form-textarea"
                      rows="2"
                      placeholder="Add investigation context, witness references, or surveillance details..."
                      value={addForm.notes}
                      onChange={(e) => setAddForm({ ...addForm, notes: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              <div className="intel-modal-footer">
                <button 
                  type="button" 
                  className="btn-intel-secondary"
                  onClick={() => setIsAddModalOpen(false)}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="btn-intel-primary"
                  disabled={formSubmitting}
                >
                  {formSubmitting ? 'Recording Point...' : 'Save Location Point'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
