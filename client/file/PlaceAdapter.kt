package com.example.kakaoapisample

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kakaoapisample.databinding.ItemPlaceBinding

// 데이터(List<Place>)를 받아서 목록(RecyclerView)에 끼워주는 연결 장치(Adapter)
// 리스트를 관리하고 클릭 이벤트를 처리하는 어댑터
class PlaceAdapter(val onClick: (Place) -> Unit) : RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    private var placeList = listOf<Place>()

    // 데이터를 갱신하는 함수
    fun submitList(list: List<Place>) {
        placeList = list
        notifyDataSetChanged() // 화면 새로고침!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = placeList[position]
        holder.bind(place)
    }

    override fun getItemCount(): Int = placeList.size

    inner class PlaceViewHolder(private val binding: ItemPlaceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(place: Place) {
            binding.tvPlaceName.text = place.placeName
            binding.tvAddress.text = place.roadAddressName // 도로명 주소

            // 목록을 클릭했을 때 실행할 동작
            binding.root.setOnClickListener {
                onClick(place)
            }
        }
    }
}