import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { ArrowRight, Heart, LogOut, Menu, Package, Search, Shield, ShoppingBag, User, X } from 'lucide-react'
import { CapMark } from './ui'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useWishlist } from '../context/WishlistContext'
import { useStoreConfig } from '../lib/hooks'
import { inr } from '../lib/format'

function Logo({ name, inverted = false }) {
  return (
    <Link to="/" className="group flex shrink-0 items-center gap-2" aria-label={`${name} home`}>
      <CapMark className="h-7 w-8 transition-transform duration-200 group-hover:-rotate-6" color={inverted ? '#ffffff' : '#1f38d3'} />
      <span className={`font-display text-xl font-semibold uppercase tracking-[-0.02em] ${inverted ? 'text-white' : 'text-primary'}`}>{name}</span>
    </Link>
  )
}

function AnnouncementBar() {
  const cfg = useStoreConfig()
  const msg = cfg?.freeShippingAbove != null ? `Free shipping on orders above ${inr(cfg.freeShippingAbove)}` : 'Made in our own workshop'
  return (
    <div className="bg-secondary text-white">
      <p className="page flex h-9 items-center justify-center gap-2 text-center text-xs font-medium uppercase tracking-[0.08em]">
        {msg}<span className="hidden text-white/50 sm:inline">·</span><Link to="/products?sort=newest" className="hidden underline-offset-4 hover:underline sm:inline">Shop new arrivals</Link>
      </p>
    </div>
  )
}

function SearchForm({ onDone, autoFocus }) {
  const nav = useNavigate()
  const [q, setQ] = useState('')
  const submit = (e) => {
    e.preventDefault()
    nav(`/products${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`)
    setQ('')
    onDone?.()
  }
  return (
    <form onSubmit={submit} role="search" className="flex gap-2">
      <label className="sr-only" htmlFor={autoFocus ? 'site-search-input' : 'mobile-search-input'}>Search caps</label>
      <div className="relative min-w-0 flex-1">
        <Search size={18} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-ink-faint" aria-hidden="true" />
        <input id={autoFocus ? 'site-search-input' : 'mobile-search-input'} autoFocus={autoFocus} className="input pl-10" placeholder="Search caps, snapbacks, bucket hats…" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>
      <button type="submit" className="btn-primary" aria-label="Search"><ArrowRight size={18} /></button>
    </form>
  )
}

function Navbar() {
  const { user, logout, isAdmin } = useAuth()
  const { cart } = useCart()
  const { ids } = useWishlist()
  const cfg = useStoreConfig()
  const nav = useNavigate()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  const [menu, setMenu] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const menuRef = useRef(null)
  const count = cart.totalQuantity ?? cart.items.length
  const storeName = cfg?.storeName || 'CapStore'

  // Close every overlay on navigation.
  useEffect(() => { setOpen(false); setMenu(false); setSearchOpen(false) }, [pathname])
  useEffect(() => {
    if (!menu && !searchOpen && !open) return undefined
    const onKey = (e) => { if (e.key === 'Escape') { setMenu(false); setSearchOpen(false); setOpen(false) } }
    const onClick = (e) => { if (menu && menuRef.current && !menuRef.current.contains(e.target)) setMenu(false) }
    document.addEventListener('keydown', onKey)
    document.addEventListener('mousedown', onClick)
    return () => { document.removeEventListener('keydown', onKey); document.removeEventListener('mousedown', onClick) }
  }, [menu, searchOpen, open])
  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  const links = [
    ['/', 'Home'],
    ['/products', 'Shop'],
    ['/about', 'About'],
    ['/contact', 'Contact'],
  ]
  const linkCls = ({ isActive }) => `relative py-2 text-sm font-medium uppercase tracking-[0.06em] transition-colors after:absolute after:inset-x-0 after:-bottom-px after:h-0.5 after:origin-left after:bg-primary after:transition-transform ${isActive ? 'text-primary after:scale-x-100' : 'text-ink hover:text-primary after:scale-x-0 hover:after:scale-x-100'}`
  const iconBtn = 'relative flex h-10 w-10 items-center justify-center rounded-sm text-ink transition-colors hover:bg-twill hover:text-primary'

  return (
    <header className="sticky top-0 z-40 border-b border-tertiary bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/85">
      <div className="page relative flex h-16 items-center gap-4">
        <button className={`${iconBtn} -ml-2 lg:hidden`} onClick={() => setOpen(true)} aria-label="Open menu" aria-expanded={open}><Menu size={22} /></button>
        <Logo name={storeName} />
        <nav className="absolute left-1/2 hidden -translate-x-1/2 items-center gap-9 lg:flex" aria-label="Main">
          {links.map(([to, label]) => <NavLink key={to} to={to} end={to === '/'} className={linkCls}>{label}</NavLink>)}
        </nav>
        <div className="ml-auto flex items-center gap-0.5">
          <button type="button" onClick={() => setSearchOpen((v) => !v)} className={`${iconBtn} hidden sm:flex`} aria-label={searchOpen ? 'Close search' : 'Open search'} aria-expanded={searchOpen} aria-controls="site-search">
            {searchOpen ? <X size={20} /> : <Search size={20} />}
          </button>
          <Link to="/wishlist" className={iconBtn} aria-label={`Wishlist${ids.size ? `, ${ids.size} saved` : ''}`}>
            <Heart size={20} />
            {ids.size > 0 && <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-primary" aria-hidden="true" />}
          </Link>
          <Link to="/cart" className={iconBtn} aria-label={`Cart, ${count} ${count === 1 ? 'item' : 'items'}`}>
            <ShoppingBag size={20} />
            {count > 0 && <span className="badge absolute -right-0.5 -top-0.5 min-w-[20px] justify-center px-1.5">{count > 99 ? '99+' : count}</span>}
          </Link>
          {user ? (
            <div className="relative" ref={menuRef}>
              <button onClick={() => setMenu(!menu)} className={`${iconBtn} w-auto gap-2 px-2`} aria-expanded={menu} aria-haspopup="menu" aria-label="Account menu">
                <User size={20} /><span className="hidden max-w-[110px] truncate text-sm font-medium lg:inline">{user.name.split(' ')[0]}</span>
              </button>
              {menu && (
                <div role="menu" className="absolute right-0 z-50 mt-2 w-60 animate-fade-up overflow-hidden rounded-md border border-tertiary bg-white py-1 shadow-[0_12px_32px_-12px_rgba(40,40,40,.25)]">
                  <div className="border-b border-tertiary px-4 py-3">
                    <p className="truncate text-sm font-semibold">{user.name}</p>
                    <p className="truncate text-xs text-ink-faint">{user.email}</p>
                  </div>
                  {[
                    ['/orders', 'My orders', Package],
                    ['/account', 'Account & addresses', User],
                    ...(isAdmin ? [['/admin', 'Admin panel', Shield]] : []),
                  ].map(([to, label, Icon]) => (
                    <Link key={to} role="menuitem" className="flex items-center gap-3 px-4 py-2.5 text-sm hover:bg-twill" to={to}><Icon size={16} className="text-ink-faint" />{label}</Link>
                  ))}
                  <button role="menuitem" className="flex w-full items-center gap-3 border-t border-tertiary px-4 py-2.5 text-left text-sm hover:bg-twill" onClick={() => { logout(); nav('/') }}><LogOut size={16} className="text-ink-faint" />Log out</button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/login" className="btn-primary btn-sm ml-2 hidden sm:inline-flex">Log in</Link>
          )}
        </div>
        {searchOpen && (
          <div id="site-search" className="absolute inset-x-0 top-full animate-fade-up border-b border-tertiary bg-white py-4 shadow-[0_12px_24px_-16px_rgba(40,40,40,.25)]">
            <div className="mx-auto max-w-2xl px-4"><SearchForm autoFocus onDone={() => setSearchOpen(false)} /></div>
          </div>
        )}
      </div>

      {/* Mobile drawer */}
      {open && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu">
          <div className="absolute inset-0 bg-ink/40" onClick={() => setOpen(false)} />
          <div className="absolute inset-y-0 left-0 flex w-[min(22rem,88vw)] animate-fade-up flex-col bg-white">
            <div className="flex h-16 items-center justify-between border-b border-tertiary px-4">
              <Logo name={storeName} />
              <button className={iconBtn} onClick={() => setOpen(false)} aria-label="Close menu"><X size={22} /></button>
            </div>
            <div className="border-b border-tertiary p-4"><SearchForm onDone={() => setOpen(false)} /></div>
            <nav className="flex flex-1 flex-col overflow-y-auto p-2" aria-label="Mobile">
              {[...links, ...(user ? [['/orders', 'My orders'], ['/account', 'Account']] : [])].map(([to, label]) => (
                <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `flex items-center justify-between rounded-sm px-3 py-3.5 font-display text-lg font-medium uppercase ${isActive ? 'text-primary' : 'text-ink hover:bg-twill'}`}>
                  {label}<ArrowRight size={18} className="text-ink-faint" />
                </NavLink>
              ))}
            </nav>
            <div className="border-t border-tertiary p-4">
              {user
                ? <button className="btn-outline w-full" onClick={() => { logout(); nav('/') }}><LogOut size={16} /> Log out</button>
                : <div className="grid grid-cols-2 gap-2"><Link to="/login" className="btn-primary">Log in</Link><Link to="/register" className="btn-outline">Sign up</Link></div>}
            </div>
          </div>
        </div>
      )}
    </header>
  )
}

function Footer() {
  const cfg = useStoreConfig()
  const storeName = cfg?.storeName || 'CapStore'
  const cols = [
    ['Shop', [['/products', 'All caps'], ['/products?sort=newest', 'New arrivals'], ['/wishlist', 'Wishlist'], ['/cart', 'Cart']]],
    ['Account', [['/account', 'My account'], ['/orders', 'My orders'], ['/register', 'Create an account']]],
    ['Help', [['/orders', 'Track an order'], ['/about', 'About us'], ['/contact', 'Contact us']]],
  ]
  return (
    <footer className="mt-24 bg-secondary text-white">
      <div className="page grid gap-10 py-14 md:grid-cols-[1.4fr_repeat(3,1fr)]">
        <div>
          <Logo name={storeName} inverted />
          <p className="mt-4 max-w-xs text-sm leading-6 text-white/70">Caps made in our own workshop, shipped straight to your door.</p>
        </div>
        {cols.map(([title, items]) => (
          <div key={title}>
            <h4 className="mb-4 text-xs font-semibold uppercase tracking-[0.08em] text-white/50">{title}</h4>
            <ul className="space-y-2.5 text-sm">
              {items.map(([to, label]) => <li key={label}><Link to={to} className="text-white/85 transition-colors hover:text-white">{label}</Link></li>)}
            </ul>
          </div>
        ))}
      </div>
      <div className="border-t border-white/10">
        <div className="page flex flex-col items-center justify-between gap-2 py-5 text-xs text-white/50 sm:flex-row">
          <p>© {new Date().getFullYear()} {storeName}. All rights reserved.</p>
          <p>Secure payments by Razorpay · GST invoices on every order</p>
        </div>
      </div>
    </footer>
  )
}

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-2 focus:top-2 focus:z-[60] focus:rounded-sm focus:bg-white focus:p-3 focus:shadow">Skip to content</a>
      <AnnouncementBar />
      <Navbar />
      <main id="main" className="flex-1"><Outlet /></main>
      <Footer />
    </div>
  )
}
