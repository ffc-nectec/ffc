/*
 * Copyright (c) 2018 NECTEC
 *   National Electronics and Computer Technology Center, Thailand
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ffc.app.location

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.SearchView.OnQueryTextListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ffc.android.addVeriticalItemDivider
import ffc.android.observe
import ffc.android.viewModel
import ffc.app.FamilyFolderActivity
import ffc.app.R
import ffc.entity.gson.toJson
import ffc.entity.place.House
import kotlinx.android.synthetic.main.item_house.view.*
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.dimen
import org.jetbrains.anko.find
import org.jetbrains.anko.toast

class HouseNoLocationActivtiy : FamilyFolderActivity() {

    private var houseAdapter: HouseAdapter? = null
    private val viewModel by lazy { viewModel<HouseViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_house_no_location)
        setSupportActionBar(find(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val searchView = find<SearchView>(R.id.searchView)
        searchView.queryHint = getString(R.string.search_house_hint)
        searchView.setOnQueryTextListener(object : OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                houseAdapter?.filter = query
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                houseAdapter?.filter = newText
                return true
            }
        })

        observe(viewModel.houses) {
            if (!it.isNullOrEmpty()) {
                setupListOf(it)
            }
        }

        housesOf(org!!).houseNoLocation {
            onFound { viewModel.houses.value = it }
            onNotFound { viewModel.houses.value = listOf() }
            onFail { toast(it.message ?: "onFailure") }
        }
    }

    private fun setupListOf(houses: List<House>) {
        val houseList = find<RecyclerView>(R.id.recycleView)
        houseAdapter = HouseAdapter(houses) {
            val bundle = bundleOf("house" to it.toJson())
            setResult(Activity.RESULT_OK, Intent().apply { putExtras(bundle) })
            finish()
        }
        houseAdapter?.onEmptyHouse = { }
        houseList.adapter = houseAdapter
        houseList.addVeriticalItemDivider(dimen(R.dimen.content_start_horizontal_padding))
        houseList.layoutManager = LinearLayoutManager(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    class HouseViewModel : ViewModel() {
        val houses = MutableLiveData<List<House>>()
    }

    class HouseViewHolder(view: View, val onItemClick: (House) -> Unit) : RecyclerView.ViewHolder(view) {
        fun bind(address: House) {
            with(address) {
                itemView.houseNo.text = no
                itemView.setOnClickListener { onItemClick(this) }
            }
        }
    }

    class HouseAdapter(
        private val houses: List<House>,
        private val onItemClick: (House) -> Unit
    ) : RecyclerView.Adapter<HouseViewHolder>() {

        var onEmptyHouse: (() -> Unit)? = null
        var filteredHouse: List<House> = ArrayList()
        var filter: String? = null
            set(value) {
                field = value
                if (value != null)
                    filteredHouse = houses.filter { it.no?.contains(value) ?: false }
                else
                    filteredHouse = ArrayList(houses)
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_house, parent, false)
            return HouseViewHolder(view, onItemClick)
        }

        override fun getItemCount(): Int {
            val size = filteredHouse.size
            if (size == 0)
                onEmptyHouse?.invoke()
            return filteredHouse.size
        }

        override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
            holder.bind(filteredHouse[position])
        }
    }
}
