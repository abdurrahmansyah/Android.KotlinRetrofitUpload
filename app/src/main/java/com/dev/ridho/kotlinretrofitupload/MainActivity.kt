package com.dev.ridho.kotlinretrofitupload

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.widget.Toast
import com.dev.ridho.kotlinretrofitupload.Remote.IUploadAPI
import com.dev.ridho.kotlinretrofitupload.Remote.RetrofitClient
import com.dev.ridho.kotlinretrofitupload.Utils.ProgressRequestBody
import com.ipaulpro.afilechooser.utils.FileUtils
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Callback
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity(), ProgressRequestBody.UploadCallBacks {
    override fun onProgressUpdate(percentage: Int) {
        dialog.progress = percentage
    }

    val BASE_URL = "http://192.168.2.227:100" //my laptop ip address

    val apiUpload:IUploadAPI
        get() = RetrofitClient.getClient(BASE_URL).create(IUploadAPI::class.java)


    private val PERMISSION_REQUEST: Int = 1000
    private val PICK_IMAGE_REQUEST: Int = 1001

    lateinit var mService:IUploadAPI
    lateinit var dialog: ProgressDialog
    private var selectedFileUri: Uri?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Request runtime permission
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST)

        //Service
        mService = apiUpload

        image_view.setOnClickListener { chooseImage() }

        btn_upload.setOnClickListener { uploadFile() }
    }

    private fun uploadFile() {
        if(selectedFileUri != null){
            dialog = ProgressDialog(this@MainActivity)
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            dialog.setMessage("Uploading...")
            dialog.isIndeterminate = false
            dialog.setCancelable(false)
            dialog.max = 100
            dialog.show()

            val file = FileUtils.getFile(this, selectedFileUri)
            val requestFile = ProgressRequestBody(file, this)

            val body = MultipartBody.Part.createFormData("uploaded_file", file.name, requestFile)

            Thread(Runnable {
                mService.uploadFile(body)
                    .enqueue(object: retrofit2.Callback<String> {
                        override fun onFailure(call: Call<String>, t: Throwable) {
                            dialog.dismiss()
                            Toast.makeText(this@MainActivity, t!!.message, Toast.LENGTH_SHORT).show()
                        }

                        override fun onResponse(call: Call<String>, response: Response<String>) {
                            dialog.dismiss()
                            Toast.makeText(this@MainActivity, "Upload Successfully", Toast.LENGTH_SHORT).show()
                        }

                    })
            }).start()
        }
        else
            Toast.makeText(this, "Plase choose file by click to Image", Toast.LENGTH_SHORT).show()
    }

    private fun chooseImage() {
        val getContentIntent= FileUtils.createGetContentIntent()
        val intent = Intent.createChooser(getContentIntent, "Select a file")
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == PICK_IMAGE_REQUEST){
                if(data != null){
                    selectedFileUri = data.data
                    if(selectedFileUri != null && !selectedFileUri!!.path.isEmpty())
                        image_view.setImageURI(selectedFileUri)
                }
            }
        }
    }

    //Ctrl+O
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_REQUEST -> {
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
