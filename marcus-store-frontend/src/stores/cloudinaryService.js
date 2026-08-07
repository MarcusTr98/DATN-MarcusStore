import axios from "axios"

const CLOUD_NAME = "dyeb3lju6"
const IMAGE_PRESET = "review_images"
const VIDEO_PRESET = "video_unsigned"

async function upload(file) {
    const isVideo = file.type.startsWith('video/')
    const uploadPreset = isVideo ? VIDEO_PRESET : IMAGE_PRESET
    const resourceType = isVideo ? 'video' : 'image'

    const formData = new FormData()
    formData.append("file", file)
    formData.append("upload_preset", uploadPreset)

    const res = await axios.post(
        `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/${resourceType}/upload`,
        formData
    )

    return res.data.secure_url
}

export default {
    upload
}