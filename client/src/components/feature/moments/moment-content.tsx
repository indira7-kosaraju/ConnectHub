import type { MomentDto } from '@/lib/api/schema';
import { cn } from '@/lib/utils';

/** Renders a moment's body (photo or text on gradient) filling its container. */
export function MomentContent({ moment, size = 'full', onLoad }: { moment: MomentDto; size?: 'full' | 'tile'; onLoad?: () => void }) {
  if (moment.kind === 'image' && moment.imageUrl) {
    return (
      <div className="relative size-full overflow-hidden bg-[#121110]">
        {size === 'full' && (
          <img src={moment.imageUrl} alt="" aria-hidden className="absolute inset-0 size-full scale-110 object-cover opacity-50 blur-2xl" />
        )}
        <img
          src={moment.imageUrl}
          alt={moment.text ?? 'Moment photo'}
          onLoad={onLoad}
          className={cn('relative size-full', size === 'full' ? 'object-contain' : 'object-cover')}
          loading={size === 'tile' ? 'lazy' : 'eager'}
        />
        {moment.text && size === 'full' && (
          <p className="absolute inset-x-0 bottom-24 mx-auto w-fit max-w-[85%] rounded-xl bg-[#121110]/60 px-4 py-2 text-center text-base text-white backdrop-blur">
            {moment.text}
          </p>
        )}
      </div>
    );
  }
  return (
    <div className={cn('grid size-full place-items-center p-8', `moment-bg-${moment.background ?? 'sunset'}`)}>
      <p
        className={cn(
          'whitespace-pre-line break-words text-center font-serif font-semibold leading-snug text-white',
          size === 'full' ? 'text-3xl sm:text-4xl' : 'line-clamp-5 text-sm',
        )}
      >
        {moment.text}
      </p>
    </div>
  );
}
