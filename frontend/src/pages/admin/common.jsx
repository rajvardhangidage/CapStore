export function PageHeader({ title, children, sub }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div><h1 className="font-display text-2xl font-semibold uppercase sm:text-3xl">{title}</h1>{sub && <p className="text-sm text-ink-soft">{sub}</p>}</div>
      <div className="flex flex-wrap items-center gap-2">{children}</div>
    </div>
  )
}

/** Scrolls sideways on small screens instead of breaking the page layout */
export function Table({ head, children, empty, minWidth = 'min-w-[640px]' }) {
  const rows = Array.isArray(children) ? children.length : children ? 1 : 0
  return (
    <div className="card overflow-x-auto">
      <table className={`w-full ${minWidth}`}>
        <thead className="border-b border-tertiary bg-twill/60"><tr>{head.map((h) => <th key={h} className="th">{h}</th>)}</tr></thead>
        <tbody className="divide-y divide-tertiary [&>tr:hover]:bg-twill/50">{children}</tbody>
      </table>
      {rows === 0 && <p className="p-8 text-center text-sm text-ink-faint">{empty || 'Nothing to show.'}</p>}
    </div>
  )
}
