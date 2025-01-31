package com.ndridm.mynotes

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ndridm.mynotes.adapter.NoteAdapter
import com.ndridm.mynotes.databinding.ActivityMainBinding
import com.ndridm.mynotes.db.NoteHelper
import com.ndridm.mynotes.entity.Note
import com.ndridm.mynotes.helper.MappingHelper
import com.ndridm.mynotes.helper.NightModeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter

    val resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            // akan dipanggil jika request code ADD
            when (result.resultCode) {
                NoteAddUpdateActivity.RESULT_ADD -> {
                    val note = result.data?.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
                    adapter.addItem(note)
                    binding.rvNotes.smoothScrollToPosition(adapter.itemCount - 1)
                    showSnackbarMessage("Satu item berhasil ditambahkan")
                }
                NoteAddUpdateActivity.RESULT_UPDATE -> {
                    val note = result.data?.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
                    val position = result.data?.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0) as Int
                    adapter.updateItem(position, note)
                    binding.rvNotes.smoothScrollToPosition(position)
                    showSnackbarMessage("Satu item berhasil diubah")
                }
                NoteAddUpdateActivity.RESULT_DELETE -> {
                    val position = result.data?.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0) as Int
                    adapter.removeItem(position)
                    showSnackbarMessage("Satu item berhasil dihapus")
                }
            }
        }
    }

    companion object {
        private const val EXTRA_STATE = "EXTRA_STATE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            title = "Notes"
            setDisplayHomeAsUpEnabled(false)
        }

        NightModeHelper.applyTheme(this)

        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.setHasFixedSize(true)

        adapter = NoteAdapter(object :NoteAdapter.OnItemClickCallback {
            override fun onItemClicked(selectedNote: Note?, position: Int?) {
                val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_NOTE, selectedNote)
                intent.putExtra(NoteAddUpdateActivity.EXTRA_POSITION, position)
                resultLauncher.launch(intent)
            }

        })

        binding.rvNotes.adapter = adapter

        binding.fabAdd.setOnClickListener{
            val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
            resultLauncher.launch(intent)
        }

        if (savedInstanceState == null) {
            // proses ambil data
            loadNotesAsync()
        } else {
            val list = savedInstanceState.getParcelableArrayList<Note>(EXTRA_STATE)
            if (list != null) {
                adapter.listNotes = list
            }
        }
    }

    private fun loadNotesAsync() {
        lifecycleScope.launch {
            binding.progressbar.visibility = View.VISIBLE
            val noteHelper = NoteHelper.getInstance(applicationContext)
            noteHelper.open()
            val deferredNotes = async(Dispatchers.IO) {
                val cursor = noteHelper.queryAll()
                MappingHelper.mapCursorToArrayList(cursor)
            }

            binding.progressbar.visibility = View.INVISIBLE
            val notes = deferredNotes.await()
            if (notes.size > 0) {
                adapter.listNotes = notes
            } else {
                adapter.listNotes = ArrayList()
                showSnackbarMessage("Tidak ada data saat ini")
            }
            noteHelper.close()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(EXTRA_STATE, adapter.listNotes)
    }

    private fun showSnackbarMessage(message: String) {
        Snackbar.make(binding.rvNotes, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.switch_icon)?.let { setIcon(it) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.switch_icon -> {
                val isNightMode = !NightModeHelper.loadThemeMode(this)
                NightModeHelper.saveThemeMode(this, isNightMode)
                AppCompatDelegate.setDefaultNightMode(
                    if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                // Perbarui ikon setelah tema berubah
                setIcon(item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setIcon(menuItem: MenuItem?) {
        menuItem?.icon = ContextCompat.getDrawable(
            this,
            if (NightModeHelper.loadThemeMode(this)) R.drawable.baseline_mode_night
            else R.drawable.baseline_light_mode
        )
    }

}