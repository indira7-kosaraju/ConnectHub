import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/ui/page-header';
import { EmptyState } from '@/components/ui/empty-state';
import { BookmarkIllustration } from '@/components/ui/illustrations';
import { Button } from '@/components/ui/button';
import { PostList } from '@/components/feature/posts/post-list';
import { useSavedPosts } from '@/hooks/use-posts';

export function SavedPage() {
  const saved = useSavedPosts();
  return (
    <>
      <PageHeader title="Saved" description="Posts you’ve kept for later. Only you can see this." />
      <PostList
        query={saved}
        empty={
          <EmptyState
            illustration={BookmarkIllustration}
            title="Nothing saved yet"
            description="Tap the bookmark on any post to keep it here."
            action={
              <Button asChild variant="outline">
                <Link to="/">Back to your feed</Link>
              </Button>
            }
          />
        }
      />
    </>
  );
}
