import axios from "axios"

const CLOUD_NAME = "dyeb3lju6"
const UPLOAD_PRESET = "review_images"

async function upload(file) {

    const formData = new FormData()

    formData.append("file", file)
    formData.append("upload_preset", UPLOAD_PRESET)

    const res = await axios.post(

        `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`,

        formData

    )

    return res.data.secure_url

}

export default {

    upload

}