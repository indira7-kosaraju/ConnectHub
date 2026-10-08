import { useState } from 'react';
import { motion } from 'framer-motion';
import { Bookmark, MessageCircle } from 'lucide-react';
import { PageHeader } from '@/components/ui/page-header';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { FormField } from '@/components/ui/form-field';
import { Badge } from '@/components/ui/badge';
import { Avatar } from '@/components/ui/avatar';
import { VibeRing } from '@/components/ui/vibe-ring';
import { toast } from '@/components/ui/toaster';
import { PostCardSkeleton } from '@/components/feature/post-card-skeleton';
import { ThemeToggle } from '@/components/feature/theme-toggle';
import { EMPTY_VIBES, VIBES, VIBE_META, type Vibe, type VibeCounts } from '@/types/vibe';
import { cn } from '@/lib/utils';

/** Dev-only living style guide for reviewing the design system. */
export function DesignPage() {
  const [counts, setCounts] = useState<VibeCounts>({ ...EMPTY_VIBES, spark: 6, deep: 3, support: 2 });
  const [mine, setMine] = useState<Vibe | null>(null);

  const react = (v: Vibe) => {
    setCounts((c) => {
      const next = { ...c };
      if (mine) next[mine] -= 1;
      if (mine !== v) next[v] += 1;
      return next;
    });
    setMine((m) => (m === v ? null : v));
  };

  return (
    <div className="space-y-8">
      <PageHeader title="Design system" description="Tokens, primitives and motion." actions={<ThemeToggle />} />

      <section className="space-y-3">
        <h2 className="text-xl font-semibold">Buttons</h2>
        <div className="flex flex-wrap gap-2">
          <Button>Primary</Button>
          <Button variant="accent">Accent</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="outline">Outline</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="danger">Danger</Button>
          <Button loading>Saving</Button>
          <Button variant="outline" onClick={() => toast.success('Saved to your collection')}>
            Show toast
          </Button>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-xl font-semibold">Vibe ring</h2>
        <Card interactive>
          <CardHeader>
            <div className="flex items-center gap-3">
              <Avatar name="Maya Okafor" />
              <div>
                <p className="text-sm font-medium">Maya Okafor</p>
                <p className="text-xs text-muted">in Film Photography · 2h</p>
              </div>
              <Badge variant="secondary" className="ml-auto">
                Pinned
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-[15px] leading-relaxed">
              Shot a whole roll of Portra 400 at golden hour and every frame came back warmer than I remembered the light
              being. Memory is a filter too.
            </p>
            <div className="mt-5 flex items-center gap-3">
              <VibeRing counts={counts} className="size-11" thickness={3}>
                <span className="grid size-8 place-items-center rounded-full bg-surface-2 text-base" aria-hidden>
                  {mine ? VIBE_META[mine].glyph : '✦'}
                </span>
              </VibeRing>
              <div className="flex gap-1" role="group" aria-label="React">
                {VIBES.map((v, i) => (
                  <motion.button
                    key={v}
                    type="button"
                    initial={{ opacity: 0, y: 6, scale: 0.8 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{ delay: i * 0.04, type: 'spring', stiffness: 400, damping: 22 }}
                    onClick={() => react(v)}
                    aria-pressed={mine === v}
                    aria-label={VIBE_META[v].label}
                    className={cn(
                      'grid size-9 place-items-center rounded-full text-lg transition-colors hover:bg-surface-2',
                      mine === v && 'bg-surface-2 ring-2 ring-line-strong',
                    )}
                    style={{ color: VIBE_META[v].color }}
                  >
                    {VIBE_META[v].glyph}
                  </motion.button>
                ))}
              </div>
              <Button variant="ghost" size="icon-sm" className="ml-auto" aria-label="Comments">
                <MessageCircle />
              </Button>
              <Button variant="ghost" size="icon-sm" aria-label="Save">
                <Bookmark />
              </Button>
            </div>
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Form field</CardTitle>
            <CardDescription>Label, hint and error are wired with aria.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <FormField label="Username" hint="Lowercase letters, numbers and underscores.">
              <Input placeholder="maya_shoots" />
            </FormField>
            <FormField label="Email" error="That email is already in use.">
              <Input defaultValue="maya@example.com" />
            </FormField>
          </CardContent>
        </Card>
        <PostCardSkeleton withMedia />
      </section>
    </div>
  );
}
