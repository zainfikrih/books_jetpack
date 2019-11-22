package dominando.android.livros

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dominando.android.livros.common.BaseFragment
import dominando.android.livros.databinding.FragmentBookListBinding
import dominando.android.presentation.BookListViewModel
import dominando.android.presentation.ViewState
import org.koin.android.ext.android.inject

class BookListFragment : BaseFragment() {
    private val viewModel: BookListViewModel by inject()

    private lateinit var rvBooks : RecyclerView

    private val bookAdapter by lazy {
        BookAdapter { book ->
            router.showBookDetails(book)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_book_list,
                container,
                false) as FragmentBookListBinding

        return binding.run {
            lifecycleOwner = this@BookListFragment
            viewModel      = this@BookListFragment.viewModel

            /* Init UI */
            initRecyclerView(rvBooks)
            initFab(fabAdd)

            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_sign_out) {
            auth.signOut()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun init() {
        viewModel.state().observe(viewLifecycleOwner, Observer { viewState ->
            viewState?.let {
                if (viewState.status == ViewState.Status.ERROR) {
                    Toast.makeText(requireContext(),
                            R.string.message_error_load_books,
                            Toast.LENGTH_SHORT).show()
                }
            }
        })
        viewModel.removeOperation().observe(viewLifecycleOwner, Observer { event ->
            event.consumeEvent()?.let { viewState ->
                when (viewState.status) {
                    ViewState.Status.SUCCESS -> {
                        Snackbar.make(rvBooks, R.string.message_book_removed, Snackbar.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(requireContext(),
                                R.string.message_error_delete_book, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
        lifecycle.addObserver(viewModel)
    }

    private fun initRecyclerView(recyclerView: RecyclerView) {
        rvBooks = recyclerView
        recyclerView.adapter = bookAdapter
        attachSwipeToRecyclerView()
    }

    private fun initFab(fab: FloatingActionButton) {
        fab.setOnClickListener {
            router.showBookForm(null)
        }
    }

    private fun attachSwipeToRecyclerView() {
        val swipe = object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                val position = viewHolder.adapterPosition
                deleteBookFromPosition(position)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipe)
        itemTouchHelper.attachToRecyclerView(rvBooks)
    }

    private fun deleteBookFromPosition(position: Int) {
        bookAdapter.getBook(position)?.let { book ->
            viewModel.remove(book)
        }
    }
}
