package com.application.informationsupport.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.*
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.application.informationsupport.R
import com.application.informationsupport.database.DatabaseConnector
import com.application.informationsupport.models.ModelChat
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter (val context: Context, val chatList: MutableList<ModelChat>, val currentName: String,
                   private val url: String?,
                   private val username: String?,
                   private val pass: String?) : RecyclerView.Adapter<ChatAdapter.MyHolder>() {

    private var currentAnimator: Animator? = null
    private var shortAnimationDuration: Int = (context as Activity).resources.getInteger(android.R.integer.config_shortAnimTime)

    class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var loginTV: TextView = itemView.findViewById(R.id.nameTV)
        var messageTV: TextView = itemView.findViewById(R.id.messageTV)
        var timeTV: TextView = itemView.findViewById(R.id.timeTV)
        var messageLayout: CardView = itemView.findViewById(R.id.card)
        var messageIV: ImageView = itemView.findViewById(R.id.messageIV)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        if (viewType == MESSAGE_TYPE_RIGHT) {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_chat_right,
                parent, false
            )
            return MyHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(
                R.layout.row_chat_left,
                parent, false
            )
            return MyHolder(view)
        }
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val message = chatList[position].message
        val timestamp = chatList[position].timeStamp
        val image = chatList[position].image
        val login = chatList[position].sender
        if (message != null) {
            holder.messageTV.text = message
            holder.messageTV.visibility = View.VISIBLE
        }
        else {
            holder.messageTV.visibility = View.GONE
        }
        if (image != null) {
            holder.messageIV.setImageBitmap(image)
            holder.messageIV.visibility = View.VISIBLE
            holder.messageIV.setOnClickListener {
                zoomImageFromThumb(it, image)
            }
        }
        else {
            holder.messageIV.visibility = View.GONE
        }
        holder.loginTV.text = login
        holder.timeTV.text = timestamp
        holder.messageLayout.setOnClickListener {
            if (currentName == login) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Удаление")
                builder.setMessage("Вы точно хотите удалить данное сообщение?")
                builder.setPositiveButton("Да", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        deleteMessage(chatList[position].id)
                    }

                })
                builder.setNegativeButton("Нет", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        dialog!!.dismiss()
                    }

                })
                builder.create().show()
            }
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].sender == currentName) {
            MESSAGE_TYPE_LEFT
        } else {
            MESSAGE_TYPE_RIGHT
        }
    }

    private fun deleteMessage(id: Int) {
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            connection.createStatement()
                .executeQuery("update messages set changeddate = SYSTIMESTAMP, deleted = 1 where idmessage = $id")
            Toast.makeText(context, "Сообщение удалено", Toast.LENGTH_SHORT)
                .show()
            connection.close()
        } catch (e: Exception) {
            val file = File(context.filesDir, "log_error")
            if (!file.exists()) {
                file.mkdir()
            }
            try {
                val logfile = File(file, "log")
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
                    myOutWriter.close()
                    fout.close()
                } else {
                    val writer = FileWriter(logfile)
                    writer.append(date.toString())
                    writer.append("\n")
                    writer.append(e.toString())
                    writer.flush()
                    writer.close()
                }
            } catch (e: Exception) {

            }
        }
    }

    fun updateChat() {
        chatList.clear()
        try {
            val connection = DatabaseConnector(url, username, pass).createConnection()
            val userRS = connection.createStatement().executeQuery("select * from users where login = '$currentName'")
            userRS.next()
            val chatlistRS = connection.createStatement().executeQuery("select * from messages where deleted = 0 and createdby in (select iduser from users where service = ${userRS.getString("service")} and district = ${userRS.getString("district")}) order by creationdate")
            while (chatlistRS.next()) {
                val createdLogRS = connection.createStatement().executeQuery("select login from users where iduser = ${chatlistRS.getString("createdby")}")
                createdLogRS.next()
                val createdLog = createdLogRS.getString("login")
                val image = chatlistRS.getBlob("image")
                var scaledBitmap: Bitmap? = null
                if (image != null) {
                    val bytes = image.getBytes(1L, image.length().toInt())
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, false)
                }
                chatList.add(ModelChat(chatlistRS.getString("text"), createdLog, chatlistRS.getString("creationdate").split(".")[0], scaledBitmap, chatlistRS.getInt("idmessage")))
            }
            notifyDataSetChanged()
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
            context.run {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun zoomImageFromThumb(thumbView: View, imageBitmap: Bitmap) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        currentAnimator?.cancel()

        // Load the high-resolution "zoomed-in" image.
        val expandedImageView: ImageView = (context as Activity).findViewById(R.id.expanded_image)
        expandedImageView.setImageBitmap(imageBitmap)

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBoundsInt)
        context.findViewById<View>(R.id.container)
            .getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            // Extend start bounds horizontally
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        expandedImageView.visibility = View.VISIBLE

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.pivotX = 0f
        expandedImageView.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        currentAnimator = AnimatorSet().apply {
            play(
                ObjectAnimator.ofFloat(
                expandedImageView,
                View.X,
                startBounds.left,
                finalBounds.left)
            ).apply {
                with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f))
            }
            duration = shortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
        }

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        expandedImageView.setOnClickListener {
            currentAnimator?.cancel()

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            currentAnimator = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left)).apply {
                    with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale))
                }
                duration = shortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }
                })
                start()
            }
        }
    }

    companion object {
        const val MESSAGE_TYPE_LEFT = 0
        const val MESSAGE_TYPE_RIGHT = 1
    }
}