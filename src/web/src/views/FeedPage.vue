<script setup>import {computed, onMounted, onUnmounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {useUserStore} from '../stores/user';
import {getFeed} from '../api/feed';
import {createComment, favoriteVideo, getComments, likeVideo, unfavoriteVideo, unlikeVideo} from '../api/interaction';
import {followUser, unfollowUser} from '../api/relation';
import {reportViewEvent} from '../api/exposure';

const DEFAULT_AVATAR = 'data:image/svg+xml;charset=UTF-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><rect width="40" height="40" fill="%232a2d35"/><circle cx="20" cy="15" r="7" fill="%238b8fa3"/><path d="M8 35c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="%238b8fa3"/></svg>';

const props = defineProps({
  scene: {
    type: String,
    default: 'recommend'
  }
});
const router = useRouter();
const userStore = useUserStore();
const items = ref([]);
const currentIndex = ref(0);
const liked = ref({});
const favorited = ref({});
const following = ref({});
const commentsOpen = ref(false);
const comments = ref([]);
const commentsState = ref('idle');
const commentsError = ref('');
const commentsNextCursor = ref('');
const commentsHasMore = ref(false);
const loadingMoreComments = ref(false);
const commentText = ref('');
const feedState = ref('loading');
const feedError = ref('');
const nextCursor = ref('');
const hasMore = ref(false);
const loadingMore = ref(false);
const followBusyId = ref(0);
const followError = ref('');
const busyComment = ref(false);
const watchStart = ref(0);
const sceneLabels = {
  timeline: '最新视频',
  recommend: '推荐流',
  following: '关注流',
  hot: '热门榜单'
};
const currentSceneLabel = computed(() => sceneLabels[props.scene] || '推荐流');
const current = computed(() => items.value[currentIndex.value] || null);
const requiresAuthFeed = computed(() => {
  return true;
});

function generateIdempotencyKey() {
  return `pigfeed-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
}

async function loadFeed() {
  if (requiresAuthFeed.value && !userStore.token) {
    items.value = [];
    currentIndex.value = 0;
    feedState.value = 'auth';
    return;
  }
  feedState.value = 'loading';
  feedError.value = '';
  try {
    const data = await getFeed({
      scene: props.scene,
      limit: 10
    });
    items.value = data.items || [];
    liked.value = {};
    favorited.value = {};
    following.value = {};
    for (const item of items.value) {
      liked.value[item.videoId] = item.liked || false;
      favorited.value[item.videoId] = item.favorited || false;
      following.value[item.authorId] = item.following || false;
    }
    currentIndex.value = 0;
    nextCursor.value = data.nextCursor || '';
    hasMore.value = Boolean(data.hasMore && data.nextCursor);
    feedState.value = 'ready';
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
      return;
    }
    feedError.value = error.message || `${currentSceneLabel.value}加载失败`;
    feedState.value = 'error';
  }
}

async function loadMore() {
  if (loadingMore.value || feedState.value !== 'ready' || !hasMore.value) {
    return;
  }
  loadingMore.value = true;
  try {
    const data = await getFeed({
      scene: props.scene,
      cursor: nextCursor.value,
      limit: 10
    });
    const newItems = data.items || [];
    for (const item of newItems) {
      liked.value[item.videoId] = item.liked || false;
      favorited.value[item.videoId] = item.favorited || false;
      following.value[item.authorId] = item.following || false;
    }
    items.value = [...items.value, ...newItems];
    nextCursor.value = data.nextCursor || '';
    hasMore.value = Boolean(data.hasMore && data.nextCursor);
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
    }
  } finally {
    loadingMore.value = false;
  }
}

async function setLike() {
  if (!current.value)
    return;
  const videoId = current.value.videoId;
  const nextLiked = !liked.value[videoId];
  try {
    if (nextLiked) {
      await likeVideo(videoId);
    } else {
      await unlikeVideo(videoId);
    }
    liked.value[videoId] = nextLiked;
    current.value.likeCount = nextLiked
      ? (current.value.likeCount || 0) + 1
      : Math.max((current.value.likeCount || 0) - 1, 0);
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
    }
  }
}

async function setFavorite() {
  if (!current.value)
    return;
  const videoId = current.value.videoId;
  const nextFavorited = !favorited.value[videoId];
  try {
    if (nextFavorited) {
      await favoriteVideo(videoId);
    } else {
      await unfavoriteVideo(videoId);
    }
    favorited.value[videoId] = nextFavorited;
    current.value.favoriteCount = nextFavorited
      ? (current.value.favoriteCount || 0) + 1
      : Math.max((current.value.favoriteCount || 0) - 1, 0);
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
    }
  }
}

async function setFollow() {
  if (!current.value)
    return;
  if (current.value.authorId === userStore.user?.id)
    return;
  const authorId = current.value.authorId;
  const nextFollowing = !following.value[authorId];
  followBusyId.value = authorId;
  followError.value = '';
  try {
    if (nextFollowing) {
      await followUser(authorId);
    } else {
      await unfollowUser(authorId);
    }
    following.value[authorId] = nextFollowing;
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
      return;
    }
    followError.value = error.message || '关注操作失败';
  } finally {
    followBusyId.value = 0;
  }
}

async function loadComments() {
  if (!current.value)
    return;
  commentsState.value = 'loading';
  commentsError.value = '';
  commentsNextCursor.value = '';
  commentsHasMore.value = false;
  loadingMoreComments.value = false;
  try {
    // 首次请求 cursor 必须为空字符串
    const data = await getComments(current.value.videoId, {limit: 50, cursor: ''});
    comments.value = data.items || [];
    commentsNextCursor.value = data.nextCursor || '';
    commentsHasMore.value = Boolean(data.hasMore && data.nextCursor);
    commentsState.value = 'ready';
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
      return;
    }
    comments.value = [];
    commentsNextCursor.value = '';
    commentsHasMore.value = false;
    commentsError.value = error.message || '评论加载失败';
    commentsState.value = 'error';
  }
}

async function loadMoreComments() {
  if (loadingMoreComments.value || commentsState.value !== 'ready' || !commentsHasMore.value) {
    return;
  }
  loadingMoreComments.value = true;
  try {
    const data = await getComments(current.value.videoId, {
      limit: 50,
      cursor: commentsNextCursor.value
    });
    const newItems = data.items || [];
    comments.value = [...comments.value, ...newItems];
    commentsNextCursor.value = data.nextCursor || '';
    commentsHasMore.value = Boolean(data.hasMore && data.nextCursor);
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
    }
  } finally {
    loadingMoreComments.value = false;
  }
}

function handleCommentScroll(event) {
  const el = event.target;
  if (!el)
    return;
  const nearBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 80;
  if (nearBottom) {
    loadMoreComments();
  }
}

async function submitComment() {
  if (!current.value || !userStore.token)
    return;
  const content = commentText.value.trim();
  if (!content)
    return;
  busyComment.value = true;
  try {
    const data = await createComment(current.value.videoId, {content});
    comments.value = [data, ...comments.value];
    commentText.value = '';
    if (current.value) {
      current.value.commentCount = (current.value.commentCount || 0) + 1;
    }
  } catch (error) {
    if (error.message && error.message.includes('401')) {
      userStore.logout();
      router.push('/auth');
      return;
    }
    commentsError.value = error.message || '评论发布失败';
  } finally {
    busyComment.value = false;
  }
}

function goNext() {
  if (currentIndex.value < items.value.length - 1) {
    currentIndex.value++;
  }
}

function goPrev() {
  if (currentIndex.value > 0) {
    currentIndex.value--;
  }
}

const wheelLock = ref(false);

function handleWheel(event) {
  if (commentsOpen.value || wheelLock.value) return;
  if (event.deltaY > 0) {
    goNext();
  } else if (event.deltaY < 0) {
    goPrev();
  } else {
    return;
  }
  wheelLock.value = true;
  setTimeout(() => { wheelLock.value = false; }, 600);
}

function handleKeydown(event) {
  if (commentsOpen.value) return;
  if (event.key === 'ArrowDown' || event.key === 'ArrowRight') {
    goNext();
  } else if (event.key === 'ArrowUp' || event.key === 'ArrowLeft') {
    goPrev();
  }
}

function handleOpenUser(userId) {
  if (userId === userStore.user?.id) {
    router.push('/profile');
  } else {
    router.push(`/users/${userId}`);
  }
}

function isVideoSource(url) {
  if (!url)
    return false;
  return /\.(mp4|webm|ogg|mov|avi)$/i.test(url);
}

watch(() => props.scene, () => {
  loadFeed();
});
watch(currentIndex, (newIdx, oldIdx) => {
  // 上报上一个视频的观看时长
  if (oldIdx !== undefined && oldIdx !== newIdx && userStore.token) {
    const prevItem = items.value[oldIdx];
    if (prevItem && watchStart.value > 0) {
      const watchMs = Date.now() - watchStart.value;
      if (watchMs > 0) {
        const completed = watchMs >= 3000;
        reportViewEvent({
          videoId: prevItem.videoId,
          scene: props.scene,
          requestId: generateIdempotencyKey(),
          eventType: completed ? 'complete' : 'skip',
          watchMs,
          completed
        }).catch(() => {});
      }
    }
  }
  // 上报新视频的曝光事件
  if (current.value && userStore.token) {
    watchStart.value = Date.now();
    const requestId = generateIdempotencyKey();
    reportViewEvent({
      videoId: current.value.videoId,
      scene: props.scene,
      requestId,
      eventType: 'exposed',
      watchMs: 0,
      completed: false
    }).catch(() => {});
  }
  if (newIdx >= items.value.length - 3) {
    loadMore();
  }
});
watch(commentsOpen, (open) => {
  if (open) {
    loadComments();
  }
});
watch(current, (newVal, oldVal) => {
  // 切换视频时，如果评论面板正开着，重新加载新视频的评论
  if (commentsOpen.value && newVal && newVal.videoId !== oldVal?.videoId) {
    loadComments();
  }
});
onMounted(() => {
  loadFeed();
  window.addEventListener('keydown', handleKeydown);
});
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
  // 页面离开时上报最后一个视频的观看时长
  if (current.value && userStore.token && watchStart.value > 0) {
    const watchMs = Date.now() - watchStart.value;
    if (watchMs > 0) {
      const completed = watchMs >= 3000;
      reportViewEvent({
        videoId: current.value.videoId,
        scene: props.scene,
        requestId: generateIdempotencyKey(),
        eventType: completed ? 'complete' : 'skip',
        watchMs,
        completed
      }).catch(() => {});
    }
  }
});
</script>

<template>
  <main class="feed-layout">
    <section class="feed-main">
      <div v-if="feedState === 'loading'" class="feed-message">
        <span class="material-symbols-outlined">hourglass_top</span>
        <strong>正在加载{{ currentSceneLabel }}</strong>
      </div>

      <div v-else-if="feedState === 'auth'" class="feed-message">
        <span class="material-symbols-outlined">lock</span>
        <strong>登录后查看{{ currentSceneLabel }}</strong>
        <button @click="router.push('/auth')">登录</button>
      </div>

      <div v-else-if="feedState === 'error'" class="feed-message">
        <span class="material-symbols-outlined">sync_problem</span>
        <strong>{{ feedError }}</strong>
        <button @click="loadFeed">重新加载</button>
      </div>

      <div v-else-if="feedState === 'ready' && items.length === 0" class="feed-message">
        <span class="material-symbols-outlined">video_library</span>
        <strong>{{ currentSceneLabel }}暂无视频</strong>
        <button @click="loadFeed">刷新</button>
      </div>

      <div v-else-if="current" class="feed-stage-wrap" @wheel.prevent="handleWheel">
        <article class="video-stage">
          <img class="stage-backdrop" :src="current.coverUrl" alt=""/>
          <div class="stage-vignette"/>

          <video
              v-if="isVideoSource(current.mediaUrl)"
              class="stage-media"
              :src="current.mediaUrl"
              :poster="current.coverUrl"
              autoplay
              muted
              loop
              playsinline
          />
          <img v-else class="stage-media portrait-media" :src="current.mediaUrl" alt=""/>

          <div class="stage-copy">
            <div class="creator-row">
              <button
                  class="creator-profile-button"
                  type="button"
                  @click="handleOpenUser(current.authorId)"
              >
                <img :src="current.authorAvatarUrl || DEFAULT_AVATAR" alt="" @error="$event.target.src = DEFAULT_AVATAR"/>
                <strong>@{{ current.authorNickname }}</strong>
              </button>
              <button
                  class="follow-button"
                  :class="{ active: following[current.authorId] }"
                  type="button"
                  @click="setFollow"
                  :disabled="followBusyId === current.authorId || current.authorId === userStore.user?.id"
              >
                {{
                  current.authorId === userStore.user?.id ? '本人' : followBusyId === current.authorId ? '处理中' : following[current.authorId] ? '已关注' : '关注'
                }}
              </button>
            </div>
            <p v-if="followError" class="stage-notice">{{ followError }}</p>
            <h1>{{ current.title }}</h1>
            <p>{{ current.description }}</p>
          </div>

          <div class="action-rail">
            <button
                class="rail-button"
                :class="{ active: liked[current.videoId] }"
                @click="setLike"
            >
              <span class="material-symbols-outlined" :class="{ filled: liked[current.videoId] }">
                favorite
              </span>
              <strong>{{ current.likeCount || 0 }}</strong>
            </button>
            <button class="rail-button" @click="commentsOpen = true">
              <span class="material-symbols-outlined">chat_bubble</span>
              <strong>{{ current.commentCount || 0 }}</strong>
            </button>
            <button
                class="rail-button"
                :class="{ active: favorited[current.videoId] }"
                @click="setFavorite"
            >
              <span class="material-symbols-outlined" :class="{ filled: favorited[current.videoId] }">
                bookmark
              </span>
              <strong>{{ current.favoriteCount || 0 }}</strong>
            </button>
            <button class="rail-button compact">
              <span class="material-symbols-outlined">share</span>
            </button>
          </div>
        </article>

        <button
            v-if="currentIndex > 0"
            class="feed-nav-btn feed-nav-up"
            aria-label="上一个"
            @click="goPrev"
        >
          <span class="material-symbols-outlined">keyboard_arrow_up</span>
        </button>
        <button
            v-if="currentIndex < items.length - 1"
            class="feed-nav-btn feed-nav-down"
            aria-label="下一个"
            @click="goNext"
        >
          <span class="material-symbols-outlined">keyboard_arrow_down</span>
        </button>

        <div v-if="loadingMore" class="feed-loading-pill">加载中</div>
        <div v-else-if="feedState === 'ready' && items.length > 0 && !hasMore && currentIndex === items.length - 1"
             class="feed-loading-pill">已到末尾
        </div>
      </div>
    </section>

    <aside class="comment-panel" :class="{ open: commentsOpen }">
      <header class="comment-header">
        <h2>评论 <span>{{ current?.commentCount || 0 }}</span></h2>
        <div>
          <button class="icon-button small" aria-label="关闭评论" @click="commentsOpen = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </header>
      <div class="comment-list" @scroll="handleCommentScroll">
        <div v-if="commentsState === 'loading'" class="comment-empty">
          <span class="material-symbols-outlined">hourglass_top</span>
          <strong>正在加载评论</strong>
        </div>
        <div v-else-if="commentsState === 'error'" class="comment-empty">
          <span class="material-symbols-outlined">sync_problem</span>
          <strong>{{ commentsError || '评论加载失败' }}</strong>
          <button @click="loadComments">重试</button>
        </div>
        <div v-else-if="comments.length === 0" class="comment-empty">
          <span class="material-symbols-outlined">chat_bubble</span>
          <strong>暂无评论</strong>
        </div>
        <article v-for="comment in comments" :key="comment.id" class="comment-item">
          <button class="comment-user-button" type="button" @click="handleOpenUser(comment.userId)">
            <img :src="comment.userAvatarUrl || DEFAULT_AVATAR" alt="" @error="$event.target.src = DEFAULT_AVATAR"/>
          </button>
          <div>
            <div class="comment-meta">
              <button type="button" @click="handleOpenUser(comment.userId)">
                {{ comment.userNickname }}
              </button>
            </div>
            <p>{{ comment.content }}</p>
          </div>
        </article>
        <div v-if="loadingMoreComments" class="comment-empty small">
          <span class="material-symbols-outlined">hourglass_top</span>
          <strong>加载中</strong>
        </div>
        <div v-else-if="commentsState === 'ready' && comments.length > 0 && !commentsHasMore" class="comment-empty small">
          <strong>已到末尾</strong>
        </div>
      </div>
      <form class="comment-form" @submit.prevent="submitComment">
        <img :src="userStore.user?.avatarUrl || DEFAULT_AVATAR" alt="" @error="$event.target.src = DEFAULT_AVATAR"/>
        <input
            v-model="commentText"
            :placeholder="userStore.token ? '添加评论...' : '登录后评论'"
            :disabled="!userStore.token || busyComment"
        />
        <button :disabled="!userStore.token || !commentText.trim() || busyComment" aria-label="发送评论">
          <span class="material-symbols-outlined">send</span>
        </button>
      </form>
    </aside>
  </main>
</template>
