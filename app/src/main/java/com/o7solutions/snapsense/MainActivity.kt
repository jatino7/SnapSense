package com.o7solutions.snapsense

import android.app.Dialog
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ImageView
import com.google.firebase.firestore.FirebaseFirestore

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.o7solutions.snapsense.Utils.AppConstants
import com.o7solutions.snapsense.Utils.AppFunctions
import com.o7solutions.snapsense.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(this, R.color.white)
        )

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            showIconDialog()
        }

        lifecycleScope.launch {
            val key = getApiKey()
            AppFunctions.saveApiKey(this@MainActivity, key.toString())
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun showIconDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.icon_dialog)

        // Make the dialog background semi-transparent

        // Setup icons
        val qr = dialog.findViewById<ImageView>(R.id.qr)
        val camera = dialog.findViewById<ImageView>(R.id.camera)
        val image = dialog.findViewById<ImageView>(R.id.image)
        val chat = dialog.findViewById<ImageView>(R.id.chat)
        val genPic = dialog.findViewById<ImageView>(R.id.genPic)

        qr.setOnClickListener {

            val navOptions = NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    false
                ) // Pops everything above start destination
                .build()

            navController.navigate(R.id.QRFragment, null, navOptions)
            dialog.dismiss()
        }
        camera.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    false
                ) // Pops everything above start destination
                .build()

            navController.navigate(R.id.cameraFragment, null, navOptions)
            dialog.dismiss()
        }
        image.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    false
                ) // Pops everything above start destination
                .build()

            navController.navigate(R.id.FirstFragment, null, navOptions)
            dialog.dismiss()
        }
        chat.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    false
                ) // Pops everything above start destination
                .build()

            navController.navigate(R.id.testingFragment, null, navOptions)
            dialog.dismiss()
        }

        genPic.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    false
                ) // Pops everything above start destination
                .build()

            navController.navigate(R.id.genPicFragment, null, navOptions)
            dialog.dismiss()
        }

        dialog.show()
    }

    suspend fun getApiKey(): String? {

        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("keys")
                .document("1234")
                .get()
                .await()

            snapshot.getString("KEY_API")  // returns the value or null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}