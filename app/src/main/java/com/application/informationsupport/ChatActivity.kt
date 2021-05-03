package com.application.informationsupport

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.text.TextUtils
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.application.informationsupport.adapters.ChatAdapter
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelChat
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class ChatActivity : AppCompatActivity() {

    var image_uri: Uri? = null
    val CAMERA_REQUEST_CODE = 100
    val STORAGE_REQUEST_CODE = 200
    val IMAGE_PICK_GALLERY_CODE = 300
    val IMAGE_PICK_CAMERA_CODE = 400
    protected lateinit var storagePermissions: Array<String>
    protected lateinit var cameraPermissions: Array<String>
    protected lateinit var adapter: ChatAdapter
    var messageSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val messageET : EditText = findViewById(R.id.messageEt)
        val sendButton : ImageButton = findViewById(R.id.sendButton)
        val attachButton : ImageButton = findViewById(R.id.attachBtn)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = "Чат"
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "informationSupport",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val url = sharedPreferences.getString("URL", "")
        val username = sharedPreferences.getString("username", "")
        val pass = sharedPreferences.getString("pass", "")
        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        val recyclerView = findViewById<RecyclerView>(R.id.chat_recyclerView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager
        attachButton.setOnClickListener {
            showImagePicDialog()
        }
        sendButton.setOnClickListener {
            val message = messageET.text.toString().trim()
            if (TextUtils.isEmpty(message) && image_uri == null) {
                Toast.makeText(
                    this, "Нельзя отправить пустое сообщение",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sendMessage(message, image_uri)
            }
            messageET.setText("")
            messageSent = true
        }
        adapter = ChatAdapter(this, mutableListOf(), intent.getStringExtra("name")!!, url, username, pass)
        recyclerView.adapter = adapter
        adapter.updateChat()
        Timer("UpdateChat", false).schedule(5000, 5000) {
            this@ChatActivity.runOnUiThread{
                adapter.updateChat()
                if (messageSent) {
                    recyclerView.scrollToPosition(adapter.chatList.size - 1)
                    messageSent = false
                }
            }
        }
    }

    private fun sendMessage(message: String?, image_uri: Uri?) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "informationSupport",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val url = sharedPreferences.getString("URL", "")
        val username = sharedPreferences.getString("username", "")
        val pass = sharedPreferences.getString("pass", "")
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val idStmt = connection.createStatement()
            val rs = idStmt.executeQuery(
                "select iduser from users where" +
                        " login = '${intent.getStringExtra("name")}'"
            )
            rs.next()
            val creatorID = rs.getString("iduser")
            if (image_uri != null) {
                var encodedImage: ByteArray? = null
                val bitmap =
                    MediaStore.Images.Media.getBitmap(this.contentResolver, image_uri)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos)
                encodedImage = baos.toByteArray()
                if (message != null) {
                    val preparedStatement = connection.prepareStatement("insert into messages (text, image, createdby, creationdate) values ('$message', ?, $creatorID, SYSTIMESTAMP)")
                    preparedStatement.setBinaryStream(1, ByteArrayInputStream(encodedImage))
                    preparedStatement.executeUpdate()
                }
                else {
                    val preparedStatement = connection.prepareStatement("insert into messages (image, createdby, creationdate) values (?, $creatorID, SYSTIMESTAMP)")
                    preparedStatement.setBinaryStream(1, ByteArrayInputStream(encodedImage))
                    preparedStatement.executeUpdate()
                }
            }
            else if (message != null) {
                connection.createStatement().executeQuery("insert into messages (text, createdby, creationdate) values ('$message', $creatorID, SYSTIMESTAMP)")
            }
            connection.close()
        }
        catch (e: Exception) {
            try {
                val logfile = File(Environment.getExternalStorageDirectory().absolutePath, "log.txt")
                val timestamp = System.currentTimeMillis()
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.ROOT);
                val localTime = sdf.format(Date(timestamp))
                val date = sdf.parse(localTime)!!
                if (logfile.exists()) {
                    val fout = FileOutputStream(logfile, true)
                    val myOutWriter = OutputStreamWriter(fout)
                    myOutWriter.append("\n")
                    myOutWriter.append(date.toString())
                    myOutWriter.append("\n")
                    myOutWriter.append(e.toString())
                    e.stackTrace.forEach {
                        myOutWriter.append("\n")
                        myOutWriter.append(it.toString())
                    }
                    myOutWriter.close()
                    fout.close()
                }
                else {
                    val writer = FileWriter(logfile)
                    writer.append(date.toString())
                    writer.append("\n")
                    writer.append(e.toString())
                    e.stackTrace.forEach {
                        writer.append("\n")
                        writer.append(it.toString())
                    }
                    writer.flush()
                    writer.close()
                }
            }
            catch (e: Exception) {

            }
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePicDialog() {
        val options = arrayOf("Камера", "Галерея")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Откуда получить нужно получить изображение?")
        builder.setItems(options) { _, which ->
            if (which == 0) {
                if (!checkCameraPermission()) {
                    requestCameraPermission()
                } else {
                    pickFromCamera()
                }
            } else if (which == 1) {
                if (!checkStoragePermission()) {
                    requestStoragePermission()
                } else {
                    pickFromGallery()
                }
            }
        }
        builder.create().show()
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            storagePermissions,
            STORAGE_REQUEST_CODE
        )
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val result2 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        return result && result2
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }

    private fun pickFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp description")
        image_uri =
            this.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && writeStorageAccepted) {
                        pickFromCamera()
                    } else {
                        Toast.makeText(
                            this,
                            "Please enable camera and storage permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (writeStorageAccepted) {
                        pickFromGallery()
                    } else {
                        Toast.makeText(
                            this,
                            "Please enable storage permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                image_uri = data!!.data!!
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
