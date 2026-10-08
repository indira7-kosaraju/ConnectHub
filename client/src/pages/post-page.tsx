import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PostCardSkeleton } from '@/components/feature/post-card-skeleton';
import { PostCard } from '@/components/feature/posts/post-card';
import { CommentThread } from '@/components/feature/comments/comment-thread';
import { usePost } from '@/hooks/use-posts';
import { ApiRequestError } from '@/lib/api/client';
import { NotFoundPanel } from './not-found-page';

export function PostPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const post = usePost(id);

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" className="-ml-2 text-muted" onClick={() => (window.history.length > 1 ? navigate(-1) : navigate('/'))}>
        <ArrowLeft aria-hidden /> Back
      </Button>
      {post.isPending ? (
        <PostCardSkeleton />
      ) : post.error instanceof ApiRequestError && post.error.status === 404 ? (
        <NotFoundPanel title="This post isn’t available" description="It may have been deleted, or it’s in a private circle you’re not part of." />
      ) : post.data ? (
        <>
          <PostCard post={post.data} detail />
          <CommentThread postId={post.data.id} />
        </>
      ) : (
        (() => {
          throw post.error;
        })()
      )}
    </div>
  );
}
