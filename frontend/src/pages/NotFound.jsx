import { Link } from 'react-router-dom'
import { EmptyState } from '../components/ui'

export default function NotFound() {
  return <EmptyState title="Page not found" text="That page has come unstitched."><Link to="/" className="btn-primary">Back to home</Link></EmptyState>
}
