import { Link } from 'react-router-dom'
import api from '../api'
import ProductCard from '../components/ProductCard'
import { EmptyState, ErrorBox, PageLoader } from '../components/ui'
import { useAsync } from '../lib/hooks'
import { useAuth } from '../context/AuthContext'
import { useWishlist } from '../context/WishlistContext'

export default function Wishlist() {
  const { user } = useAuth()
  const { ids } = useWishlist()
  const { data, loading, error, reload } = useAsync(() => (user ? api.get('/wishlist').then((r) => r.data) : Promise.resolve([])), [user])
  if (!user) return <EmptyState title="Your wishlist" text="Log in to save the caps you like."><Link to="/login" state={{ from: '/wishlist' }} className="btn-primary">Log in</Link></EmptyState>
  if (loading) return <PageLoader />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  const list = data.filter((p) => ids.has(p.id))
  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <h1 className="mb-6 text-3xl font-extrabold">Wishlist</h1>
      {list.length === 0 ? <EmptyState title="Nothing saved yet" text="Tap the heart on any cap to save it here."><Link to="/products" className="btn-primary">Browse caps</Link></EmptyState>
        : <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-4">{list.map((p) => <ProductCard key={p.id} p={p} />)}</div>}
    </div>
  )
}
