import type { ProviderSummary } from '../types/schema'

interface Props {
  providers: ProviderSummary[]
  activeId: string | null
  onSelect: (id: string) => void
}

export function ProviderTabs({ providers, activeId, onSelect }: Props) {
  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: '0',
      flexShrink: 0,
    }}>
      {providers.map((p, i) => {
        const isActive = p.id === activeId
        const isFirst = i === 0
        const isLast = i === providers.length - 1
        return (
          <button
            key={p.id}
            onClick={() => onSelect(p.id)}
            style={{
              padding: '5px 14px',
              fontSize: '12px',
              fontWeight: isActive ? '500' : '400',
              color: isActive ? 'var(--color-text)' : 'var(--color-text-secondary)',
              background: isActive ? 'var(--color-surface)' : 'var(--color-bg)',
              border: '1px solid var(--color-border)',
              borderLeft: isFirst ? '1px solid var(--color-border)' : 'none',
              borderRadius: isFirst
                ? 'var(--radius-md) 0 0 var(--radius-md)'
                : isLast
                ? '0 var(--radius-md) var(--radius-md) 0'
                : '0',
              cursor: 'pointer',
              transition: 'background 0.1s, color 0.1s',
              whiteSpace: 'nowrap',
            }}
          >
            {p.label}
          </button>
        )
      })}
    </div>
  )
}
