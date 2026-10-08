export function DividerLabel({ children }: { children: string }) {
  return (
    <div className="flex items-center gap-3 text-xs uppercase tracking-[0.12em] text-muted" role="separator">
      <span className="h-px flex-1 bg-line" />
      {children}
      <span className="h-px flex-1 bg-line" />
    </div>
  );
}
