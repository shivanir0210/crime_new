import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import { 
  getCases, 
  searchPublicInformation, 
  searchCasePublicInformation,
  savePublicInformation,
  getCasePublicInformation,
  updatePublicInformationItem
} from '../services/api';
import { 
  Globe, 
  Search, 
  AlertTriangle, 
  CheckCircle, 
  Bookmark, 
  ExternalLink, 
  Info, 
  Calendar, 
  FileText,
  Briefcase,
  ShieldAlert,
  RotateCw,
  Clock,
  Image as ImageIcon
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

export default function PublicNewsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const caseIdParam = searchParams.get('caseId') || '';
  const initialQuery = searchParams.get('q') || '';

  const [selectedCaseId, setSelectedCaseId] = useState(caseIdParam);
  const [casesList, setCasesList] = useState([]);
  const [loadingCases, setLoadingCases] = useState(true);

  const [query, setQuery] = useState(initialQuery);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [searchType, setSearchType] = useState('NEWS');

  const [searching, setSearching] = useState(false);
  const [searchResponse, setSearchResponse] = useState(null);
  const [searchError, setSearchError] = useState('');

  const [savedReferences, setSavedReferences] = useState([]);
  const [loadingSaved, setLoadingSaved] = useState(false);

  const [flashMessage, setFlashMessage] = useState('');

  // Load cases list
  useEffect(() => {
    async function loadCases() {
      try {
        setLoadingCases(true);
        const data = await getCases({ size: 100, sort: 'updatedAt,desc' });
        const list = data?.content || [];
        setCasesList(list);
        if (!caseIdParam && list.length > 0) {
          setSelectedCaseId(list[0].id);
        }
      } catch (err) {
        console.error('Failed to load station cases:', err);
      } finally {
        setLoadingCases(false);
      }
    }
    loadCases();
  }, []);

  // Fetch saved references when selectedCaseId changes
  const fetchSavedReferences = useCallback(async (cId) => {
    if (!cId) {
      setSavedReferences([]);
      return;
    }
    try {
      setLoadingSaved(true);
      const refs = await getCasePublicInformation(cId);
      const itemsList = Array.isArray(refs) ? refs : (refs?.content || refs?.items || refs?.references || []);
      setSavedReferences(itemsList);
    } catch (err) {
      console.error('Failed to fetch saved public information references:', err);
      setSavedReferences([]);
    } finally {
      setLoadingSaved(false);
    }
  }, []);

  useEffect(() => {
    if (selectedCaseId) {
      fetchSavedReferences(selectedCaseId);
    }
  }, [selectedCaseId, fetchSavedReferences]);

  // Execute Search
  const handleSearch = async (e) => {
    if (e) e.preventDefault();
    if (!query.trim() || query.trim().length < 3) {
      setSearchError('Search query must be at least 3 characters.');
      return;
    }
    if (dateFrom && dateTo && dateFrom > dateTo) {
      setSearchError('From-date cannot be later than To-date.');
      return;
    }

    try {
      setSearching(true);
      setSearchError('');
      setSearchResponse(null);

      const payload = {
        query: query.trim(),
        searchType,
        dateFrom: dateFrom || null,
        dateTo: dateTo || null,
        page: 1,
        pageSize: 10
      };

      let res;
      if (selectedCaseId) {
        res = await searchCasePublicInformation(selectedCaseId, payload);
      } else {
        res = await searchPublicInformation(payload);
      }

      setSearchResponse(res);
    } catch (err) {
      console.error('Public search error:', err);
      setSearchError(err.message || 'Public news search failed or service is unavailable.');
    } finally {
      setSearching(false);
    }
  };

  // Bookmark / Save Reference
  const handleSaveReference = async (item) => {
    if (!selectedCaseId) {
      setSearchError('Please select a case to save this reference.');
      return;
    }
    if (!item || !item.url) {
      setSearchError('Cannot save reference: missing article URL.');
      return;
    }
    try {
      setSearchError('');
      const payload = {
        title: item.title || 'Untitled Public Information',
        description: item.description || '',
        sourceName: item.sourceName || 'Unknown Source',
        sourceUrl: item.url,
        publishedAt: item.publishedAt || null,
        author: item.author || null,
        imageUrl: item.imageUrl || null,
        originalQuery: (query || '').trim(),
        notes: 'Bookmarked from Public Information Search'
      };
      await savePublicInformation(selectedCaseId, payload);
      await fetchSavedReferences(selectedCaseId);
      setFlashMessage('Public information saved successfully');
      setTimeout(() => setFlashMessage(''), 4000);
    } catch (err) {
      console.error('Failed to save public reference:', err);
      setSearchError(err.message || 'Failed to save public reference to case.');
    }
  };

  // Update Status of Saved Reference
  const handleUpdateStatus = async (refId, newStatus) => {
    if (!selectedCaseId) return;
    try {
      await updatePublicInformationItem(selectedCaseId, refId, {
        verificationStatus: newStatus,
        notes: `Verification status updated to ${newStatus}`
      });
      await fetchSavedReferences(selectedCaseId);
      setFlashMessage(`Reference status updated to ${newStatus}`);
      setTimeout(() => setFlashMessage(''), 4000);
    } catch (err) {
      console.error('Failed to update status:', err);
      setSearchError(err.message || 'Failed to update reference status');
    }
  };

  return (
    <div className="pn-page-wrapper">
      <TopBar />

      <div className="pn-page-body">
        <Sidebar />

        <main className="pn-page-content">
          {/* Header */}
          <div className="pn-header">
            <div className="pn-header-left">
              <div className="pn-icon-box">
                <Globe size={22} />
              </div>
              <div>
                <h1 className="pn-title">PUBLIC INFORMATION SEARCH</h1>
                <p className="pn-subtitle">Search publicly available information for investigative context</p>
              </div>
            </div>

            {/* Case selector */}
            <div className="pn-case-select-group">
              <label htmlFor="pn-case-select">Case Context:</label>
              <select
                id="pn-case-select"
                value={selectedCaseId}
                onChange={(e) => {
                  setSelectedCaseId(e.target.value);
                  if (e.target.value) {
                    setSearchParams({ caseId: e.target.value });
                  } else {
                    setSearchParams({});
                  }
                }}
                className="pn-select"
                disabled={loadingCases}
              >
                <option value="">-- General Search (No Case Attached) --</option>
                {casesList.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.caseNumber} — {c.title}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Responsible AI Disclaimer Banner */}
          <div className="pn-ai-disclaimer">
            <Info size={18} className="pn-ai-icon" />
            <div>
              <strong>RESPONSIBLE AI &amp; LEGAL ADVISORY:</strong> Public news is investigative context only. It is not automatically treated as official evidence. Investigators must independently verify source reliability, relevance, and authenticity before using it in a case.
            </div>
          </div>

          {/* Flash message */}
          {flashMessage && (
            <div className="pn-alert pn-alert-success">
              <CheckCircle size={16} />
              <span>{flashMessage}</span>
            </div>
          )}

          {/* Search Toolbar Panel */}
          <form onSubmit={handleSearch} className="pn-search-panel">
            <div className="pn-search-row">
              <div className="pn-input-wrapper">
                <Search size={16} className="pn-search-icon" />
                <input
                  type="text"
                  placeholder="Enter keywords, incident locations, names, or forensic queries..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="pn-search-input"
                />
              </div>

              <button
                type="submit"
                disabled={searching}
                className="pn-search-btn"
              >
                {searching ? <RotateCw size={15} className="spin-icon" /> : <Search size={15} />}
                <span>{searching ? 'SEARCHING...' : 'SEARCH PUBLIC INFORMATION'}</span>
              </button>
            </div>

            {/* Additional Filter Row */}
            <div className="pn-filters-row">
              <div className="pn-filter-item">
                <label>Date From:</label>
                <input
                  type="date"
                  value={dateFrom}
                  onChange={(e) => setDateFrom(e.target.value)}
                  className="pn-date-input"
                />
              </div>

              <div className="pn-filter-item">
                <label>Date To:</label>
                <input
                  type="date"
                  value={dateTo}
                  onChange={(e) => setDateTo(e.target.value)}
                  className="pn-date-input"
                />
              </div>

              <div className="pn-filter-item">
                <label>Type:</label>
                <select
                  value={searchType}
                  onChange={(e) => setSearchType(e.target.value)}
                  className="pn-date-input"
                >
                  <option value="NEWS">News Articles Only</option>
                  <option value="WEB">Web Intelligence</option>
                </select>
              </div>
            </div>
          </form>

          {/* Error Alert */}
          {searchError && (
            <div className="pn-alert pn-alert-error">
              <AlertTriangle size={16} />
              <span>{searchError}</span>
            </div>
          )}

          {/* MOCK MODE BADGE */}
          {searchResponse && searchResponse.mocked && (
            <div className="pn-mock-badge">
              <ShieldAlert size={16} />
              <span>{searchResponse.mockMessage || 'DEVELOPMENT MOCK — PUBLIC NEWS NOT FETCHED'}</span>
            </div>
          )}

          {/* SEARCH RESULTS SECTION */}
          {searching ? (
            <div className="pn-empty-box">
              <RotateCw size={32} className="spin-icon" style={{ color: 'var(--bright-crime-red)' }} />
              <div className="pn-empty-title">Searching Public News &amp; Web Sources...</div>
              <div className="pn-empty-desc">Fetching external articles matching your investigative criteria.</div>
            </div>
          ) : searchResponse ? (
            <div className="pn-results-container">
              <div className="pn-results-header">
                <span>PUBLIC SEARCH RESULTS ({searchResponse.totalResults} FOUND)</span>
                <span className="pn-provider-tag">Provider: {searchResponse.provider ? (searchResponse.provider === 'newsapi' ? 'NEWS_API' : searchResponse.provider.toUpperCase()) : 'NEWS_API'}</span>
              </div>

              {(searchResponse?.results || []).length === 0 ? (
                <div className="pn-empty-box">
                  <FileText size={36} style={{ color: '#A7A7AA', opacity: 0.6 }} />
                  <div className="pn-empty-title">No public news articles found matching query.</div>
                  <div className="pn-empty-desc">Try broadening your keywords or adjusting the date range filters.</div>
                </div>
              ) : (
                <div className="pn-results-grid">
                  {(searchResponse?.results || []).map((item, idx) => {
                    const hasVideo = Boolean((item.videoUrl && isPlayableVideoUrl(item.videoUrl)) || (item.video && isPlayableVideoUrl(item.video)));
                    const mediaUrl = item.imageUrl || item.urlToImage;

                    return (
                      <article key={item.id || idx} className="public-news-card pn-result-card">
                        <div className="public-news-meta pn-card-top">
                          <div className="public-news-source pn-card-source">{item.sourceName || 'News Source'}</div>
                          {item.publishedAt && (
                            <div className="public-news-date pn-card-date">
                              <Calendar size={12} />
                              <span>{new Date(item.publishedAt).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                            </div>
                          )}
                          {item.author && (
                            <div className="public-news-author" style={{ fontSize: '0.72rem', color: '#9CA3AF' }}>By {item.author}</div>
                          )}
                        </div>

                        <div className="public-news-content">
                          <h3 className="public-news-title pn-card-title">{item.title}</h3>
                          {item.description && (
                            <p className="public-news-description pn-card-desc">{item.description}</p>
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
                            <div className="public-news-media">
                              <img
                                src={mediaUrl}
                                alt={item.title || "Public news thumbnail"}
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

                          <div className="public-news-verification pn-card-warning">
                            <AlertTriangle size={12} />
                            <span>Public Information — Verify Before Use</span>
                          </div>

                          <div className="public-news-actions pn-card-actions">
                            {isMockPublicUrl(item.url, item.sourceName, item.title, searchResponse?.provider || item.relevanceIndicator) ? (
                              <span className="pn-btn-link mock-disabled" title="Development mock data — no external source available">
                                <Info size={13} />
                                <span>Development mock data — no external source available</span>
                              </span>
                            ) : item.url ? (
                              <a
                                href={item.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="pn-btn-link"
                              >
                                <span>Read Article</span>
                                <ExternalLink size={13} />
                              </a>
                            ) : null}

                            {selectedCaseId && (
                              <button
                                onClick={() => handleSaveReference(item)}
                                className="pn-btn-bookmark"
                                title="Bookmark reference to active case record"
                              >
                                <Bookmark size={13} />
                                <span>Save Reference</span>
                              </button>
                            )}
                          </div>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </div>
          ) : (
            <div className="pn-empty-box">
              <Globe size={40} style={{ color: 'var(--bright-crime-red)', opacity: 0.7 }} />
              <div className="pn-empty-title">Enter search keywords to query public news.</div>
              <div className="pn-empty-desc">
                Search local press releases, news reports, and public web information to gather investigative context.
              </div>
            </div>
          )}

          {/* SAVED PUBLIC REFERENCES SECTION (IF CASE SELECTED) */}
          {selectedCaseId && (
            <div className="pn-saved-section">
              <div className="pn-saved-header" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Bookmark size={16} />
                <span>SAVED PUBLIC INFORMATION REFERENCES</span>
                <span className="saved-count-badge">{savedReferences.length} RECORDED</span>
              </div>

              {loadingSaved ? (
                <div className="pn-empty-box" style={{ padding: 24 }}>
                  <RotateCw size={20} className="spin-icon" />
                  <div>Loading saved case references...</div>
                </div>
              ) : (savedReferences || []).length === 0 ? (
                <div className="pn-empty-box" style={{ padding: 24 }}>
                  <div className="pn-empty-desc">No public information references saved for this case yet.</div>
                </div>
              ) : (
                <div className="pn-saved-list">
                  {(savedReferences || []).map(ref => (
                    <div key={ref.id} className="pn-saved-item">
                      <div className="pn-saved-main">
                        <div className="pn-saved-info">
                          <h4 className="pn-saved-title">{ref.title}</h4>
                          <div className="pn-saved-meta">
                            <span>Source: {ref.sourceName}</span>
                            {isMockPublicUrl(ref.sourceUrl, ref.sourceName, ref.title) ? (
                              <span className="pn-meta-link mock-disabled" title="Development mock data — no external source available">
                                Development mock data — no external source available
                              </span>
                            ) : ref.sourceUrl ? (
                              <a href={ref.sourceUrl} target="_blank" rel="noopener noreferrer" className="pn-meta-link">
                                Link <ExternalLink size={11} />
                              </a>
                            ) : null}
                          </div>
                        </div>

                        {/* Status badge & dropdown */}
                        <div className="pn-saved-status-area">
                          <span className={`pn-status-tag status-${ref.verificationStatus}`}>
                            {ref.verificationStatus}
                          </span>

                          <select
                            value={ref.verificationStatus}
                            onChange={(e) => handleUpdateStatus(ref.id, e.target.value)}
                            className="pn-status-select"
                          >
                            <option value="UNVERIFIED">UNVERIFIED</option>
                            <option value="UNDER_REVIEW">UNDER_REVIEW</option>
                            <option value="CORROBORATED">CORROBORATED</option>
                            <option value="CONTRADICTED">CONTRADICTED</option>
                            <option value="NOT_RELEVANT">NOT_RELEVANT</option>
                          </select>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </main>
      </div>

      <style>{`
        *, *::before, *::after {
          box-sizing: border-box;
        }

        .pn-page-wrapper {
          min-height: 100vh;
          display: flex;
          flex-direction: column;
          background-color: var(--bg-primary, #080808);
          color: var(--text-primary, #F5F5F5);
          width: 100%;
          max-width: 100%;
          min-width: 0;
          overflow-x: hidden;
        }

        .pn-page-body {
          display: flex;
          flex: 1;
          width: 100%;
          max-width: 100%;
          min-width: 0;
        }

        @media (max-width: 768px) {
          .pn-page-body {
            flex-direction: column;
          }
        }

        .pn-page-content {
          flex: 1;
          padding: 24px;
          max-width: 1400px;
          margin: 0 auto;
          width: 100%;
          min-width: 0;
        }

        @media (max-width: 768px) {
          .pn-page-content {
            padding: 14px;
          }
        }

        .pn-header {
          background-color: var(--bg-panel, #121214);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: var(--panel-radius, 8px);
          padding: 18px 20px;
          margin-bottom: 20px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          flex-wrap: wrap;
          width: 100%;
          min-width: 0;
        }

        .pn-header-left {
          display: flex;
          align-items: center;
          gap: 12px;
          min-width: 0;
        }

        .pn-icon-box {
          width: 40px;
          height: 40px;
          border-radius: 8px;
          background: rgba(139, 0, 0, 0.25);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: var(--bright-crime-red, #C1121F);
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .pn-title {
          font-family: var(--font-heading);
          font-size: 1.1rem;
          font-weight: 700;
          color: var(--text-bright, #FFFFFF);
          letter-spacing: 0.06em;
          margin: 0;
          text-transform: uppercase;
        }

        .pn-subtitle {
          font-size: 0.76rem;
          color: var(--text-secondary, #A7A7AA);
          margin-top: 2px;
        }

        .pn-case-select-group {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
          min-width: 0;
        }

        .pn-case-select-group label {
          font-size: 0.78rem;
          font-family: var(--font-subheading);
          font-weight: 700;
          color: var(--text-secondary, #A7A7AA);
          text-transform: uppercase;
        }

        .pn-select {
          background-color: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 6px;
          padding: 8px 14px;
          font-size: 0.82rem;
          color: var(--text-bright, #FFFFFF);
          font-family: var(--font-body);
          outline: none;
          max-width: 100%;
          cursor: pointer;
        }

        /* Disclaimer */
        .pn-ai-disclaimer {
          background: rgba(139, 0, 0, 0.12);
          border: 1px solid rgba(193, 18, 31, 0.4);
          border-radius: 6px;
          padding: 12px 16px;
          margin-bottom: 20px;
          font-size: 0.78rem;
          color: #FCA5A5;
          display: flex;
          align-items: flex-start;
          gap: 10px;
          line-height: 1.5;
        }

        .pn-ai-icon {
          color: var(--bright-crime-red, #C1121F);
          flex-shrink: 0;
          margin-top: 2px;
        }

        /* Alert */
        .pn-alert {
          padding: 12px 16px;
          border-radius: 6px;
          font-size: 0.8rem;
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 20px;
        }

        .pn-alert-success {
          background: rgba(16, 185, 129, 0.12);
          border: 1px solid rgba(16, 185, 129, 0.4);
          color: #34D399;
        }

        .pn-alert-error {
          background: rgba(193, 18, 31, 0.15);
          border: 1px solid rgba(193, 18, 31, 0.5);
          color: #FF6B6B;
        }

        /* Mock Badge */
        .pn-mock-badge {
          background: rgba(245, 158, 11, 0.12);
          border: 1px solid rgba(245, 158, 11, 0.4);
          border-radius: 6px;
          padding: 10px 14px;
          margin-bottom: 20px;
          font-size: 0.78rem;
          font-family: var(--font-subheading);
          font-weight: 700;
          color: #FBBF24;
          display: flex;
          align-items: center;
          gap: 8px;
          letter-spacing: 0.05em;
        }

        /* Search Panel */
        .pn-search-panel {
          background-color: var(--bg-panel, #121214);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: var(--panel-radius, 8px);
          padding: 20px;
          margin-bottom: 24px;
          display: flex;
          flex-direction: column;
          gap: 14px;
          width: 100%;
          min-width: 0;
        }

        .pn-search-row {
          display: flex;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;
          width: 100%;
        }

        .pn-input-wrapper {
          flex: 1 1 300px;
          position: relative;
          min-width: 0;
        }

        .pn-search-icon {
          position: absolute;
          left: 14px;
          top: 50%;
          transform: translateY(-50%);
          color: var(--text-muted, #6F6F73);
        }

        .pn-search-input {
          width: 100%;
          padding: 10px 14px 10px 40px;
          background-color: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 6px;
          font-size: 0.85rem;
          color: var(--text-bright, #FFFFFF);
          outline: none;
          transition: border-color 0.2s;
        }

        .pn-search-input:focus {
          border-color: var(--bright-crime-red, #C1121F);
          box-shadow: 0 0 0 2px rgba(193, 18, 31, 0.2);
        }

        .pn-search-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          padding: 10px 20px;
          background: linear-gradient(135deg, var(--primary-crimson, #8B0000) 0%, var(--bright-crime-red, #C1121F) 100%);
          color: #FFFFFF;
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          border-radius: 6px;
          border: 1px solid rgba(255, 255, 255, 0.2);
          cursor: pointer;
          transition: all 0.2s;
          white-space: nowrap;
        }

        .pn-search-btn:hover:not(:disabled) {
          background: linear-gradient(135deg, #A10000 0%, #D41826 100%);
          box-shadow: 0 4px 14px rgba(193, 18, 31, 0.4);
        }

        .pn-filters-row {
          display: flex;
          align-items: center;
          gap: 16px;
          flex-wrap: wrap;
          padding-top: 10px;
          border-top: 1px solid rgba(42, 42, 45, 0.6);
        }

        .pn-filter-item {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .pn-filter-item label {
          font-size: 0.72rem;
          font-family: var(--font-subheading);
          color: var(--text-muted, #6F6F73);
          text-transform: uppercase;
        }

        .pn-date-input {
          background-color: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 4px;
          padding: 6px 10px;
          font-size: 0.78rem;
          color: var(--text-primary, #F5F5F5);
          outline: none;
        }

        /* Results Container */
        .pn-results-container {
          background-color: var(--bg-panel, #121214);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: var(--panel-radius, 8px);
          padding: 20px;
          margin-bottom: 24px;
        }

        .pn-results-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-family: var(--font-subheading);
          font-size: 0.78rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-secondary, #A7A7AA);
          padding-bottom: 14px;
          margin-bottom: 16px;
          border-bottom: 1px solid var(--border-color, #2A2A2D);
        }

        .pn-provider-tag {
          font-family: var(--font-mono);
          font-size: 0.7rem;
          color: var(--text-muted, #6F6F73);
          background: rgba(42, 42, 45, 0.6);
          padding: 2px 6px;
          border-radius: 3px;
        }

        .pn-results-grid {
          display: flex;
          flex-direction: column;
          gap: 16px;
          width: 100%;
          min-width: 0;
          max-width: 100%;
          box-sizing: border-box;
        }

        .public-news-card.pn-result-card {
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
          background-color: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 6px;
          padding: 16px;
          transition: border-color 0.2s;
        }

        .public-news-card.pn-result-card:hover {
          border-color: rgba(193, 18, 31, 0.4);
        }

        .public-news-meta.pn-card-top {
          min-width: 0;
          width: 100%;
          display: flex;
          flex-direction: column;
          align-items: flex-start;
          gap: 8px;
        }

        .public-news-source,
        .public-news-date,
        .public-news-author {
          max-width: 100%;
          overflow-wrap: anywhere;
          word-break: break-word;
          font-size: 0.74rem;
          color: var(--text-muted);
        }

        .public-news-content {
          min-width: 0;
          max-width: 100%;
          width: 100%;
          box-sizing: border-box;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .public-news-title.pn-card-title {
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

        .public-news-description.pn-card-desc {
          max-width: 100%;
          overflow-wrap: anywhere;
          word-break: break-word;
          white-space: normal;
          line-height: 1.5;
          font-size: 0.82rem;
          color: var(--text-secondary);
          margin: 0;
        }

        .public-news-media {
          width: 100%;
          max-width: 100%;
          margin-top: 12px;
          border-radius: 8px;
          overflow: hidden;
          box-sizing: border-box;
          background: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
        }

        .public-news-media img {
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

        .public-news-verification.pn-card-warning {
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

        .public-news-actions.pn-card-actions {
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
          .public-news-card.pn-result-card {
            grid-template-columns: 1fr;
            gap: 12px;
          }

          .public-news-meta.pn-card-top {
            display: flex;
            flex-direction: row;
            flex-wrap: wrap;
            gap: 8px 12px;
            align-items: center;
          }

          .public-news-media img,
          .public-news-video {
            height: 220px;
          }
        }

        .pn-card-source {
          font-family: var(--font-subheading);
          font-weight: 700;
          color: var(--bright-crime-red, #C1121F);
          text-transform: uppercase;
        }

        .pn-card-date {
          display: flex;
          align-items: center;
          gap: 4px;
          color: var(--text-muted, #6F6F73);
        }

        .pn-card-title {
          font-family: var(--font-heading);
          font-size: 0.92rem;
          font-weight: 700;
          color: var(--text-bright, #FFFFFF);
          line-height: 1.3;
          margin: 0;
        }

        .pn-card-desc {
          font-size: 0.8rem;
          color: var(--text-primary, #F5F5F5);
          line-height: 1.45;
          margin: 0;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .pn-card-warning {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.68rem;
          font-family: var(--font-subheading);
          font-weight: 700;
          color: #F59E0B;
          background: rgba(245, 158, 11, 0.1);
          padding: 4px 8px;
          border-radius: 3px;
          letter-spacing: 0.05em;
        }

        .pn-card-actions {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-top: 6px;
          flex-wrap: wrap;
        }

        .pn-btn-link {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          font-size: 0.75rem;
          color: #93C5FD;
          text-decoration: none;
          font-weight: 600;
        }

        .pn-btn-link:hover {
          text-decoration: underline;
        }

        .pn-btn-link.mock-disabled, .pn-meta-link.mock-disabled {
          color: #9CA3AF;
          font-style: italic;
          cursor: not-allowed;
          font-size: 0.72rem;
          opacity: 0.85;
        }
        .pn-btn-link.mock-disabled:hover {
          text-decoration: none;
        }

        .pn-btn-bookmark {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 12px;
          background: rgba(139, 0, 0, 0.2);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: var(--bright-crime-red, #C1121F);
          border-radius: 4px;
          font-size: 0.72rem;
          font-weight: 700;
          font-family: var(--font-subheading);
          cursor: pointer;
          transition: all 0.2s;
        }

        .pn-btn-bookmark:hover {
          background: rgba(193, 18, 31, 0.3);
          color: #FFFFFF;
        }

        /* Saved References */
        .pn-saved-section {
          background-color: var(--bg-panel, #121214);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: var(--panel-radius, 8px);
          padding: 20px;
        }

        .pn-saved-header {
          display: flex;
          align-items: center;
          gap: 8px;
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright, #FFFFFF);
          padding-bottom: 12px;
          margin-bottom: 16px;
          border-bottom: 1px solid var(--border-color, #2A2A2D);
        }

        .pn-saved-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .pn-saved-item {
          background-color: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 6px;
          padding: 12px 14px;
        }

        .pn-saved-main {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 14px;
          flex-wrap: wrap;
        }

        .pn-saved-title {
          font-family: var(--font-heading);
          font-size: 0.88rem;
          font-weight: 600;
          color: var(--text-bright, #FFFFFF);
          margin: 0 0 4px 0;
        }

        .pn-saved-meta {
          font-size: 0.72rem;
          color: var(--text-muted, #6F6F73);
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .pn-meta-link {
          color: #93C5FD;
          text-decoration: none;
          display: inline-flex;
          align-items: center;
          gap: 3px;
        }

        .pn-saved-status-area {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .pn-status-tag {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          padding: 3px 8px;
          border-radius: 3px;
          text-transform: uppercase;
        }

        .status-UNVERIFIED { background: rgba(167, 167, 170, 0.15); color: #A7A7AA; border: 1px solid #6F6F73; }
        .status-UNDER_REVIEW { background: rgba(59, 130, 246, 0.15); color: #93C5FD; border: 1px solid #3B82F6; }
        .status-CORROBORATED { background: rgba(16, 185, 129, 0.15); color: #6EE7B7; border: 1px solid #10B981; }
        .status-CONTRADICTED { background: rgba(193, 18, 31, 0.15); color: #FF6B6B; border: 1px solid #C1121F; }
        .status-NOT_RELEVANT { background: rgba(63, 63, 70, 0.4); color: #A1A1AA; border: 1px solid #52525B; }

        .pn-status-select {
          background-color: #0D0D0F;
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 4px;
          padding: 4px 8px;
          font-size: 0.72rem;
          color: var(--text-primary, #F5F5F5);
          outline: none;
        }

        .pn-empty-box {
          text-align: center;
          padding: 40px 20px;
          background-color: var(--bg-panel, #121214);
          border: 1px dashed var(--border-color, #2A2A2D);
          border-radius: var(--panel-radius, 8px);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 8px;
        }

        .pn-empty-title {
          font-size: 0.95rem;
          font-weight: 600;
          color: var(--text-bright, #FFFFFF);
        }

        .pn-empty-desc {
          font-size: 0.78rem;
          color: var(--text-secondary, #A7A7AA);
          max-width: 440px;
        }
      `}</style>
    </div>
  );
}
