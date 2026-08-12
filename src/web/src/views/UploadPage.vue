<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { uploadFile } from '../api/upload'
import { createVideo } from '../api/video'

const router = useRouter()

const videoFile = ref(null)
const videoUrl = ref('')
const videoPreview = ref('')
const uploadingVideo = ref(false)
const videoInput = ref(null)

const coverFile = ref(null)
const coverUrl = ref('')
const coverPreview = ref('')
const uploadingCover = ref(false)
const coverInput = ref(null)

const title = ref('')
const description = ref('')
const submitting = ref(false)
const submitError = ref('')

function onVideoFileChange(event) {
  const file = event.target.files[0]
  if (!file) return
  videoFile.value = file
  videoPreview.value = URL.createObjectURL(file)
}

function onCoverFileChange(event) {
  const file = event.target.files[0]
  if (!file) return
  coverFile.value = file
  coverPreview.value = URL.createObjectURL(file)
}

async function handleUploadVideo() {
  if (!videoFile.value) return
  uploadingVideo.value = true
  submitError.value = ''
  try {
    const data = await uploadFile(videoFile.value)
    videoUrl.value = data.url
  } catch (error) {
    submitError.value = error.message || '视频上传失败'
  } finally {
    uploadingVideo.value = false
  }
}

async function handleUploadCover() {
  if (!coverFile.value) return
  uploadingCover.value = true
  submitError.value = ''
  try {
    const data = await uploadFile(coverFile.value)
    coverUrl.value = data.url
  } catch (error) {
    submitError.value = error.message || '封面上传失败'
  } finally {
    uploadingCover.value = false
  }
}

async function handleSubmit() {
  if (!videoUrl.value || !coverUrl.value || !title.value.trim()) {
    submitError.value = '请先上传视频、封面，并填写标题'
    return
  }
  
  submitting.value = true
  submitError.value = ''
  
  try {
    await createVideo({
      title: title.value.trim(),
      description: description.value.trim(),
      mediaUrl: videoUrl.value,
      coverUrl: coverUrl.value
    })
    
    alert('视频发布成功！')
    router.push('/profile')
  } catch (error) {
    submitError.value = error.message || '发布失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="upload-page">
    <section class="upload-card">
      <header>
        <div>
          <p class="eyebrow">Upload</p>
          <h1>发布新视频</h1>
        </div>
      </header>
      
      <div class="upload-grid">
        <form class="upload-form" @submit.prevent="handleSubmit">
          <label>
            <span>标题</span>
            <input v-model="title" placeholder="输入视频标题" />
          </label>
          
          <label>
            <span>描述</span>
            <textarea v-model="description" placeholder="输入视频描述（可选）" />
          </label>
          
          <div class="file-picker" @click="videoInput?.click()">
            <span class="material-symbols-outlined">videocam</span>
            <div class="file-picker-copy">
              <strong>{{ videoFile?.name || '选择视频文件' }}</strong>
              <small>支持 MP4、WebM 格式</small>
            </div>
            <input
              ref="videoInput"
              type="file"
              accept="video/*"
              @change="onVideoFileChange"
              style="display: none"
            />
          </div>
          
          <button
            v-if="videoFile && !videoUrl"
            class="ghost-button"
            type="button"
            @click="handleUploadVideo"
            :disabled="uploadingVideo"
          >
            {{ uploadingVideo ? '上传中...' : '上传视频' }}
          </button>
          
          <div v-if="videoUrl" style="color: var(--cyan); font-size: 13px;">
            ✅ 视频已上传
          </div>
          
          <div class="file-picker" @click="coverInput?.click()">
            <span class="material-symbols-outlined">image</span>
            <div class="file-picker-copy">
              <strong>{{ coverFile?.name || '选择封面图片' }}</strong>
              <small>支持 JPG、PNG 格式</small>
            </div>
            <input
              ref="coverInput"
              type="file"
              accept="image/*"
              @change="onCoverFileChange"
              style="display: none"
            />
          </div>
          
          <button
            v-if="coverFile && !coverUrl"
            class="ghost-button"
            type="button"
            @click="handleUploadCover"
            :disabled="uploadingCover"
          >
            {{ uploadingCover ? '上传中...' : '上传封面' }}
          </button>
          
          <div v-if="coverUrl" style="color: var(--cyan); font-size: 13px;">
            ✅ 封面已上传
          </div>
          
          <p v-if="submitError" class="form-message">{{ submitError }}</p>
          
          <button
            class="primary-button"
            type="submit"
            :disabled="submitting || !videoUrl || !coverUrl || !title.trim()"
          >
            <span class="material-symbols-outlined">send</span>
            {{ submitting ? '发布中...' : '发布视频' }}
          </button>
        </form>
        
        <div class="upload-preview">
          <div class="preview-frame">
            <video
              v-if="videoPreview"
              :src="videoPreview"
              controls
              style="max-width: 100%; max-height: 400px;"
            />
            <img v-else-if="coverPreview" :src="coverPreview" alt="封面预览" />
            <div v-else class="upload-empty">
              <span class="material-symbols-outlined">preview</span>
              <h1>上传预览</h1>
              <p>选择视频文件后在此预览</p>
            </div>
          </div>
          
          <h2>{{ title || '视频标题' }}</h2>
          <p>{{ description || '视频描述将显示在 Feed 流中' }}</p>
          
          <div v-if="coverPreview" style="margin-top: 12px;">
            <p style="color: var(--muted); font-size: 12px;">封面预览</p>
            <img
              :src="coverPreview"
              alt="封面"
              style="max-width: 200px; border-radius: 12px; margin-top: 8px;"
            />
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
