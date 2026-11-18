import { useEffect, useMemo, useState } from "react";
import {
  ArrowRight, BadgeCheck, Bell, BriefcaseBusiness, Check,
  ChevronDown, CircleHelp, ClipboardList, Clock3, FilePlus2, FileText,
  LayoutDashboard, LogOut, Search, ShieldCheck, UserRound, X
} from "lucide-react";

const markets = ["AU", "HK", "ID", "MY", "SG", "TH"];
const statuses = {
  SUBMITTED: "Submitted",
  UNDER_REVIEW: "Under review",
  INFORMATION_REQUIRED: "Information needed",
  SETTLED: "Settled",
  REJECTED: "Rejected"
};

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    credentials: "include",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers
    }
  });
  if (response.status === 204) return null;
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(payload.message || `Request failed (${response.status})`);
    error.status = response.status;
    throw error;
  }
  return payload;
}

function App() {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);
  const [section, setSection] = useState("overview");
  const [claims, setClaims] = useState([]);
  const [selected, setSelected] = useState(null);
  const [notice, setNotice] = useState("");
  const [noticeIsError, setNoticeIsError] = useState(false);
  const isAgent = user?.role === "AGENT";

  function showNotice(message, isError = false) {
    setNotice(message);
    setNoticeIsError(isError);
  }

  useEffect(() => {
    api("/api/v1/auth/session")
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setReady(true));
  }, []);

  useEffect(() => {
    if (!notice || noticeIsError) return undefined;
    const timeoutId = window.setTimeout(() => setNotice(""), 4000);
    return () => window.clearTimeout(timeoutId);
  }, [notice, noticeIsError]);

  async function loadClaims(currentUser = user) {
    if (!currentUser) return;
    const data = currentUser.role === "AGENT"
      ? await api("/api/v1/claims/agent/queue")
      : await api("/api/v1/claims");
    setClaims(data);
    if (selected) setSelected(data.find((claim) => claim.claimId === selected.claimId) || selected);
  }

  useEffect(() => {
    if (user) loadClaims(user).catch((error) => showNotice(error.message, true));
  }, [user]);

  async function signOut() {
    await api("/api/v1/auth/logout", { method: "POST" }).catch(() => {});
    setUser(null);
    setClaims([]);
    setSelected(null);
    setSection("overview");
    setNotice("");
  }

  async function mutateClaim(path, body, successMessage) {
    try {
      const updated = await api(path, {
        method: "POST",
        ...(body === undefined ? {} : { body: JSON.stringify(body) })
      });
      setSelected(updated);
      showNotice(successMessage);
      await loadClaims();
      return updated;
    } catch (error) {
      showNotice(error.message, true);
      throw error;
    }
  }

  if (!ready) return <div className="loading-screen"><div className="brand-mark">C</div><span>Preparing your workspace…</span></div>;
  if (!user) return <AuthScreen onAuthenticated={setUser} />;

  const navigation = isAgent
    ? [["overview", "My workspace", LayoutDashboard], ["claims", "Assigned claims", ClipboardList], ["new", "Submit a claim", FilePlus2]]
    : [["overview", "Overview", LayoutDashboard], ["claims", "My claims", FileText], ["new", "Submit a claim", FilePlus2]];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark">C</div><div><strong>CHUBB</strong><small>INSURANCE PORTAL</small></div></div>
        <div className="workspace-label">WORKSPACE</div>
        <nav>
          {navigation.map(([id, label, Icon]) => (
            <button key={id} className={`nav-item ${section === id ? "active" : ""}`} onClick={() => { setSection(id); setSelected(null); }}>
              <Icon size={18} strokeWidth={1.8} /><span>{label}</span>{id === "claims" && <span className="nav-count">{claims.length}</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="help-card"><div className="help-icon"><CircleHelp size={17} /></div><strong>Need a hand?</strong><span>Our support team is here to help.</span><a href="mailto:support@example.com">Contact support <ArrowRight size={13} /></a></div>
          <div className="user-card">
            <div className="avatar">{user.fullName.slice(0, 1).toUpperCase()}</div>
            <div className="user-meta"><strong>{user.fullName}</strong><span>{isAgent ? "Claims agent" : "Customer"}</span></div>
            <button className="icon-button logout" title="Sign out" onClick={signOut}><LogOut size={17} /></button>
          </div>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb">Workspace <span>/</span> <strong>{navigation.find(([id]) => id === section)?.[1] || "Claim details"}</strong></div>
          <div className="topbar-actions"><span className={`role-chip ${isAgent ? "agent" : ""}`}><ShieldCheck size={14} />{isAgent ? "Agent portal" : "Customer portal"}</span><button className="icon-button notification" title="Notifications"><Bell size={18} /><i /></button></div>
        </header>
        <div className="page-content">
          {notice && <div className={`notice ${noticeIsError ? "error" : ""}`}><span>{notice}</span><button onClick={() => setNotice("")}><X size={15} /></button></div>}
          {selected ? (
            <ClaimDetails
              claim={selected}
              user={user}
              onBack={() => setSelected(null)}
              mutateClaim={mutateClaim}
            />
          ) : section === "new" ? (
            <SubmitClaim user={user} onSubmitted={async (claim) => { showNotice("Your claim has been submitted."); await loadClaims(); setSelected(claim); }} />
          ) : section === "claims" ? (
            <ClaimsPage claims={claims} isAgent={isAgent} onSelect={setSelected} onStartReview={async (claim) => {
              const reviewed = await mutateClaim(`/api/v1/claims/${claim.claimId}/review`, undefined, "Claim assigned to you and moved into review.");
              setSelected(reviewed);
            }} />
          ) : (
            <Overview
              user={user}
              claims={claims}
              isAgent={isAgent}
              onNew={() => setSection("new")}
              onViewAll={() => setSection("claims")}
              onSelect={setSelected}
              onStartReview={async (claim) => {
                const reviewed = await mutateClaim(`/api/v1/claims/${claim.claimId}/review`, undefined, "Claim assigned to you and moved into review.");
                setSelected(reviewed);
              }}
            />
          )}
          <footer className="page-footer"><span>© 2026 CHUBB Insurance</span><div><a href="#privacy">Privacy</a><a href="#terms">Terms</a><a href="#help">Help centre</a></div></footer>
        </div>
      </main>
    </div>
  );
}

function AuthScreen({ onAuthenticated }) {
  const [role, setRole] = useState("CUSTOMER");
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ fullName: "", email: "", password: "", agentInviteCode: "" });
  const [error, setError] = useState("");
  const [accountExists, setAccountExists] = useState(false);
  const [busy, setBusy] = useState(false);
  const agent = role === "AGENT";
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  async function submit(event) {
    event.preventDefault();
    setError("");
    setAccountExists(false);
    setBusy(true);
    try {
      const path = mode === "register" ? "/api/v1/auth/register" : "/api/v1/auth/login";
      const body = mode === "register"
        ? { ...form, role }
        : { email: form.email, password: form.password, role };
      const authenticatedUser = await api(path, { method: "POST", body: JSON.stringify(body) });
      onAuthenticated(authenticatedUser);
    } catch (requestError) {
      setError(requestError.message);
      setAccountExists(mode === "register" && requestError.status === 409);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-showcase">
        <div className="auth-brand"><div className="brand-mark">C</div><strong>CHUBB</strong></div>
        <div className="showcase-content">
          <div className="eyebrow"><span /> CLAIMS, MADE CLEAR</div>
          <h1>Here for you,<br /><em>every step</em> of the way.</h1>
          <p>One place to submit your claim, follow its progress, and get the support you need.</p>
          <div className="showcase-metrics">
            <div><strong>01</strong><span>Submit securely</span></div><div><strong>02</strong><span>Stay in the know</span></div><div><strong>03</strong><span>Move forward</span></div>
          </div>
        </div>
        <div className="showcase-bottom"><ShieldCheck size={17} /><span>Your information is protected with secure account access.</span></div>
      </div>
      <div className="auth-panel">
        <div className="auth-card">
          <div className="auth-kicker">WELCOME TO CHUBB</div>
          <h2>{mode === "login" ? "Sign in to your account" : "Create your account"}</h2>
          <p className="auth-subtitle">Choose your portal to continue.</p>
          <div className="role-selector">
            <button className={role === "CUSTOMER" ? "selected" : ""} onClick={() => setRole("CUSTOMER")} type="button"><UserRound size={17} />Customer</button>
            <button className={role === "AGENT" ? "selected" : ""} onClick={() => setRole("AGENT")} type="button"><BriefcaseBusiness size={17} />Claims agent</button>
          </div>
          <form onSubmit={submit} className="auth-form">
            {mode === "register" && <label>Full name<input value={form.fullName} onChange={set("fullName")} placeholder="Your full name" required maxLength={120} /></label>}
            <label>Email address<input value={form.email} onChange={set("email")} type="email" placeholder="you@example.com" required maxLength={254} /></label>
            <label>Password<input value={form.password} onChange={set("password")} type="password" placeholder="At least 8 characters" required minLength={8} /></label>
            {mode === "register" && agent && <label>Agent invitation code<input value={form.agentInviteCode} onChange={set("agentInviteCode")} type="password" placeholder="Provided by your administrator" required /></label>}
            {error && <div className="form-error">{error}{accountExists && <> <button className="switch" type="button" onClick={() => { setMode("login"); setError(""); setAccountExists(false); }}>Sign in</button></>}</div>}
            <button className="primary-button auth-submit" disabled={busy}>{busy ? "Please wait…" : mode === "login" ? "Sign in" : "Create account"}<ArrowRight size={17} /></button>
          </form>
          <p className="auth-switch">{mode === "login" ? "New to CHUBB?" : "Already have an account?"} <button onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); }}>{mode === "login" ? "Create an account" : "Sign in"}</button></p>
          <div className="auth-footnote"><ShieldCheck size={15} />Secure, encrypted access to your claims</div>
        </div>
        <span className="auth-copyright">© 2026 CHUBB Insurance. All rights reserved.</span>
      </div>
    </div>
  );
}

function Overview({ user, claims, isAgent, onNew, onViewAll, onSelect, onStartReview }) {
  const recent = [...claims].sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)).slice(0, 5);
  const activeCount = claims.filter((claim) => !["SETTLED", "REJECTED"].includes(claim.status)).length;
  const submittedCount = claims.filter((claim) => claim.status === "SUBMITTED").length;
  return (
    <>
      <section className="welcome-row">
        <div><div className="date-line">{new Intl.DateTimeFormat("en", { weekday: "long", month: "long", day: "numeric" }).format(new Date())}</div><h1>{isAgent ? "Good to see you," : "Welcome back,"} {user.fullName.split(" ")[0]} <span>✦</span></h1><p>{isAgent ? "Here’s what’s happening across your claims today." : "Your claims are in one place. We’re here to help."}</p></div>
        <button className="primary-button" onClick={onNew}><FilePlus2 size={17} />Start a new claim</button>
      </section>
      <section className="stat-grid">
        <StatCard label={isAgent ? "In your queue" : "Total claims"} value={claims.length} icon={FileText} tone="blue" detail={isAgent ? `${submittedCount} awaiting review` : "Across all statuses"} />
        <StatCard label="In progress" value={activeCount} icon={Clock3} tone="orange" detail="Being reviewed by our team" />
        <StatCard label="Completed" value={claims.filter((claim) => ["SETTLED", "REJECTED"].includes(claim.status)).length} icon={BadgeCheck} tone="green" detail="A decision has been made" />
      </section>
      <div className="content-grid">
        <section className="panel recent-panel">
          <div className="panel-heading"><div><h2>{isAgent ? "Claims to review" : "Your recent claims"}</h2><p>{isAgent ? "Assigned to you and awaiting assignment" : "Stay up to date on your claim progress"}</p></div><button className="text-button" onClick={onViewAll}>View all <ArrowRight size={15} /></button></div>
          {recent.length ? <ClaimTable claims={recent} isAgent={isAgent} onSelect={onSelect} onStartReview={onStartReview} /> : <EmptyState isAgent={isAgent} onNew={onNew} />}
        </section>
        <aside className="side-column">
          <section className="tip-card"><div className="tip-shape">✳</div><div className="tip-tag">A QUICK TIP</div><h3>{isAgent ? "Keep customers in the loop" : "A little detail goes a long way"}</h3><p>{isAgent ? "Clear notes and timely requests make the claims process smoother for everyone." : "Include photos and a clear description when you submit a claim to help us review it faster."}</p><div className="tip-footer"><span>CHUBB CLAIMS GUIDE</span><ArrowRight size={15} /></div></section>
          <section className="support-card"><div className="support-icon"><ShieldCheck size={18} /></div><div><strong>Here when you need us</strong><p>Questions about the claims process?</p><a href="mailto:support@example.com">Visit our help centre <ArrowRight size={13} /></a></div></section>
        </aside>
      </div>
    </>
  );
}

function StatCard({ label, value, icon: Icon, tone, detail }) {
  return <article className="stat-card"><div className={`stat-icon ${tone}`}><Icon size={19} /></div><div className="stat-label">{label}</div><div className="stat-value">{value}</div><div className="stat-detail">{detail}</div><div className={`stat-decoration ${tone}`} /></article>;
}

function ClaimsPage({ claims, isAgent, onSelect, onStartReview }) {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");
  const filtered = useMemo(() => claims.filter((claim) => {
    const text = `${claim.claimReference} ${claim.claimantName} ${claim.incidentDescription}`.toLowerCase();
    return text.includes(query.toLowerCase()) && (filter === "ALL" || claim.status === filter);
  }), [claims, query, filter]);
  return (
    <>
      <div className="page-heading"><div><div className="date-line">{isAgent ? "AGENT WORKSPACE" : "YOUR ACCOUNT"}</div><h1>{isAgent ? "Assigned claims" : "My claims"}</h1><p>{isAgent ? "Review claims assigned to you or take a new submission." : "Track each claim and see what happens next."}</p></div></div>
      <section className="panel claims-list-panel">
        <div className="list-toolbar"><div className="search-field"><Search size={16} /><input placeholder="Search by reference or name…" value={query} onChange={(event) => setQuery(event.target.value)} /></div><div className="filter-field"><span>Status</span><select value={filter} onChange={(event) => setFilter(event.target.value)}><option value="ALL">All statuses</option>{Object.entries(statuses).map(([key, value]) => <option key={key} value={key}>{value}</option>)}</select><ChevronDown size={14} /></div></div>
        {filtered.length ? <ClaimTable claims={filtered} isAgent={isAgent} onSelect={onSelect} onStartReview={onStartReview} /> : <EmptyState isAgent={isAgent} />}
      </section>
    </>
  );
}

function ClaimTable({ claims, isAgent, onSelect, onStartReview }) {
  return (
    <div className="table-scroll"><table className="claim-table"><thead><tr><th>CLAIM</th>{isAgent && <th>CUSTOMER</th>}<th>INCIDENT DATE</th><th>STATUS</th><th>UPDATED</th><th /></tr></thead><tbody>
      {claims.map((claim) => {
        const readyForReview = isAgent && claim.status === "SUBMITTED";
        const assignedToMe = isAgent && claim.assignedOfficer;
        return <tr key={claim.claimId} onClick={() => onSelect(claim)}><td><strong className="claim-ref">{claim.claimReference}</strong><span className="claim-desc">{claim.incidentDescription}</span></td>{isAgent && <td><span className="customer-name">{claim.claimantName}</span><span className="claim-desc">{claim.claimantEmail}</span></td>}<td>{formatDate(claim.incidentDate)}</td><td><StatusBadge status={claim.status} /></td><td>{formatDate(claim.updatedAt, true)}</td><td className="table-action">{readyForReview ? <button className="small-button" onClick={(event) => { event.stopPropagation(); onStartReview(claim); }}>Take claim <ArrowRight size={13} /></button> : <button className="arrow-link" aria-label="View claim"><ArrowRight size={17} /></button>}{assignedToMe && <span className="assigned-tag">YOURS</span>}</td></tr>;
      })}
    </tbody></table></div>
  );
}

function StatusBadge({ status }) {
  const tone = status === "SETTLED" ? "success" : status === "REJECTED" ? "danger" : status === "INFORMATION_REQUIRED" ? "warning" : status === "UNDER_REVIEW" ? "review" : "neutral";
  return <span className={`status-badge ${tone}`}><i />{statuses[status] || status}</span>;
}

function EmptyState({ isAgent, onNew }) {
  return <div className="empty-state"><div className="empty-icon"><FileText size={23} /></div><h3>{isAgent ? "Your queue is clear" : "No claims yet"}</h3><p>{isAgent ? "New submissions will appear here when they’re ready for review." : "When you submit a claim, you’ll be able to follow its progress here."}</p>{!isAgent && onNew && <button className="text-button" onClick={onNew}>Submit your first claim <ArrowRight size={15} /></button>}</div>;
}

function SubmitClaim({ user, onSubmitted }) {
  const [form, setForm] = useState({ claimantName: user.fullName, market: "", incidentDate: "", incidentDescription: "", estimatedLoss: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const claim = await api("/api/v1/claims/submit", { method: "POST", body: JSON.stringify({ ...form, claimantEmail: user.email, estimatedLoss: Number(form.estimatedLoss) }) });
      await onSubmitted(claim);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return <><div className="page-heading"><div><div className="date-line">NEW SUBMISSION</div><h1>Tell us what happened</h1><p>Share a few details to get your claim started. You can check its progress anytime.</p></div></div>
    <form className="panel submit-panel" onSubmit={submit}>
      <div className="form-section-title"><div className="step-number">01</div><div><h2>Your claim details</h2><p>Fields marked with * are required.</p></div></div>
      <div className="form-grid">
        <label>Claimant name *<input value={form.claimantName} onChange={set("claimantName")} required maxLength={120} /></label>
        <label>Email address<input value={user.email} disabled /></label>
        <label>Market *<select value={form.market} onChange={set("market")} required><option value="">Select a market</option>{markets.map((market) => <option key={market} value={market}>{market}</option>)}</select></label>
        <label>Incident date *<input type="date" value={form.incidentDate} max={new Date().toISOString().slice(0, 10)} onChange={set("incidentDate")} required /></label>
        <label className="wide-field">What happened? *<textarea rows="4" value={form.incidentDescription} onChange={set("incidentDescription")} placeholder="Describe the incident and any relevant details…" required maxLength={2000} /></label>
        <label>Estimated loss *<div className="currency-input"><span>$</span><input type="number" min="0.01" step="0.01" value={form.estimatedLoss} onChange={set("estimatedLoss")} placeholder="0.00" required /></div><small>Enter an estimate in your local currency.</small></label>
      </div>
      {error && <div className="form-error">{error}</div>}
      <div className="submit-footer"><span><ShieldCheck size={15} />Your information is securely stored.</span><button className="primary-button" disabled={busy}>{busy ? "Submitting…" : "Submit claim"}<ArrowRight size={16} /></button></div>
    </form>
  </>;
}

function ClaimDetails({ claim, user, onBack, mutateClaim }) {
  const [informationRequest, setInformationRequest] = useState("");
  const [decisionReason, setDecisionReason] = useState("");
  const [extraInfo, setExtraInfo] = useState("");
  const [error, setError] = useState("");
  const agent = user.role === "AGENT";
  const assignedToMe = claim.assignedOfficer === user.fullName;
  const canTake = agent && claim.status === "SUBMITTED" && !claim.assignedOfficer;
  const canDecide = agent && assignedToMe && claim.status === "UNDER_REVIEW";
  const canRequest = canDecide;
  const canRespond = !agent && claim.status === "INFORMATION_REQUIRED";

  async function action(path, body, message) {
    try {
      setError("");
      await mutateClaim(`/api/v1/claims/${claim.claimId}/${path}`, body, message);
      return true;
    } catch (requestError) {
      setError(requestError.message);
      return false;
    }
  }

  return <>
    <button className="back-link" onClick={onBack}><ArrowRight className="back-arrow" size={15} />Back to {agent ? "assigned claims" : "my claims"}</button>
    <div className="detail-heading"><div><div className="date-line">CLAIM DETAILS <span className="dot-separator">•</span> {claim.claimReference}</div><h1>{claim.claimReference}</h1><p>Submitted {formatDate(claim.submittedAt, true)}</p></div><StatusBadge status={claim.status} /></div>
    <div className="detail-grid">
      <section className="panel detail-main">
        <div className="panel-heading"><div><h2>Claim information</h2><p>Incident and submission details</p></div></div>
        <div className="detail-fields">
          <InfoField label="Claimant" value={claim.claimantName} />
          {agent && <InfoField label="Email" value={claim.claimantEmail} />}
          <InfoField label="Market" value={claim.market} />
          <InfoField label="Incident date" value={formatDate(claim.incidentDate)} />
          <InfoField label="Estimated loss" value={new Intl.NumberFormat("en", { style: "currency", currency: currencyFor(claim.market) }).format(claim.estimatedLoss)} />
          <InfoField label="Assigned officer" value={claim.assignedOfficer || "Not assigned"} />
          <InfoField label="Description" value={claim.incidentDescription} wide />
        </div>
        {claim.informationRequested && <div className="request-box"><strong>Information requested</strong><p>{claim.informationRequested}</p></div>}
        {claim.additionalInformation && <div className="request-box received"><strong>Customer response</strong><p>{claim.additionalInformation}</p></div>}
        {claim.decisionReason && <div className="request-box"><strong>{claim.status === "SETTLED" ? "Settlement note" : "Decision reason"}</strong><p>{claim.decisionReason}</p></div>}
      </section>
      <aside className="detail-side">
        <section className="panel activity-panel"><div className="panel-heading"><div><h2>Claim progress</h2><p>Latest status update</p></div></div><div className="timeline"><div className="timeline-item done"><i><Check size={12} /></i><div><strong>Claim submitted</strong><span>{formatDate(claim.submittedAt, true)}</span></div></div><div className={`timeline-item ${claim.status !== "SUBMITTED" ? "done" : "current"}`}><i>{claim.status !== "SUBMITTED" ? <Check size={12} /> : <span />}</i><div><strong>{claim.status === "SUBMITTED" ? "Awaiting review" : claim.status === "INFORMATION_REQUIRED" ? "Information requested" : "Under review"}</strong><span>{claim.assignedOfficer ? `Assigned to ${claim.assignedOfficer}` : "Our team will review your submission"}</span></div></div>{["SETTLED", "REJECTED"].includes(claim.status) && <div className="timeline-item done"><i><Check size={12} /></i><div><strong>{statuses[claim.status]}</strong><span>{formatDate(claim.updatedAt, true)}</span></div></div>}</div></section>
        {(canTake || canRequest || canDecide || canRespond) && <section className="panel action-panel"><div className="panel-heading"><div><h2>{agent ? "Take action" : "Your next step"}</h2><p>{agent ? "Update the claim as it moves forward." : "Respond to the information request."}</p></div></div>
          {canTake && <button className="primary-button full-button" onClick={() => action("review", undefined, "Claim assigned to you and moved into review.")}>Take this claim <ArrowRight size={15} /></button>}
          {canRequest && <form onSubmit={async (event) => { event.preventDefault(); if (await action("information-requests", { reason: informationRequest }, "Information request sent to the customer.")) setInformationRequest(""); }}><label>Request more information<textarea rows="3" placeholder="What information do you need?" value={informationRequest} onChange={(event) => setInformationRequest(event.target.value)} required /></label><button className="secondary-button full-button">Send request</button></form>}
          {canRespond && <form onSubmit={async (event) => { event.preventDefault(); if (await action("additional-information", { information: extraInfo }, "Your information was submitted.")) setExtraInfo(""); }}><label>Provide the requested information<textarea rows="4" placeholder="Add your response…" value={extraInfo} onChange={(event) => setExtraInfo(event.target.value)} required /></label><button className="primary-button full-button">Send information <ArrowRight size={15} /></button></form>}
          {canDecide && <><label>Decision note<textarea rows="3" placeholder="Add a note for the customer…" value={decisionReason} onChange={(event) => setDecisionReason(event.target.value)} required /></label><div className="decision-buttons"><button className="settle-button" onClick={() => action("settlement", { reason: decisionReason }, "Claim marked as settled.")}><Check size={15} />Settle claim</button><button className="reject-button" onClick={() => action("rejection", { reason: decisionReason }, "Claim rejected.")}><X size={15} />Reject</button></div></>}
          {error && <div className="form-error">{error}</div>}
        </section>}
      </aside>
    </div>
  </>;
}

function InfoField({ label, value, wide }) {
  return <div className={`info-field ${wide ? "wide-field" : ""}`}><span>{label}</span><strong>{value || "—"}</strong></div>;
}

function formatDate(value, short = false) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("en", short ? { month: "short", day: "numeric", year: "numeric" } : { month: "long", day: "numeric", year: "numeric" }).format(date);
}

function currencyFor(market) {
  return ({ AU: "AUD", HK: "HKD", ID: "IDR", MY: "MYR", SG: "SGD", TH: "THB" })[market] || "USD";
}

export default App;
