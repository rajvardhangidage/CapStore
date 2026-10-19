export function PageHeader({ title, children, sub }) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div><h1 className="text-2xl font-extrabold">{title}</h1>{sub && <p className="text-sm text-ink-soft">{sub}</p>}</div>
      <div className="flex flex-wrap items-center gap-2">{children}</div>
    </div>
  )
}

/** Scrolls sideways on small screens instead of breaking the page layout */
export function Table({ head, children, empty }) {
  const rows = Array.isArray(children) ? children.length : children ? 1 : 0
  return (
    <div className="card overflow-x-auto">
      <table className="w-full min-w-[640px]">
        <thead className="border-b border-ink/5 bg-ink/[.025]"><tr>{head.map((h) => <th key={h} className="th">{h}</th>)}</tr></thead>
        <tbody className="divide-y divide-ink/[.06]">{children}</tbody>
      </table>
      {rows === 0 && <p className="p-8 text-center text-sm text-ink-faint">{empty || 'Nothing to show.'}</p>}
    </div>
  )
}
