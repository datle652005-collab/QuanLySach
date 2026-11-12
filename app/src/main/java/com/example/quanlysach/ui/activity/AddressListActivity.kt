package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Address
import com.google.firebase.database.*

class AddressListActivity : AppCompatActivity() {

    private lateinit var rvAddress: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var ivBack: ImageView
    private lateinit var btnAddAddress: View

    private val addressList = mutableListOf<Address>()
    private var username: String = ""
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var adapter: AddressAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_list)

        // 1. Khởi tạo danh tính người dùng
        initUserIdentity()

        // 2. Ánh xạ View
        initViews()

        // 3. Thiết lập RecyclerView
        setupRecyclerView()

        // 4. Tải dữ liệu từ Firebase
        loadAddresses()

        // 5. Thiết lập các sự kiện Click
        setupClickListeners()
    }

    private fun initUserIdentity() {
        username = intent.getStringExtra("USERNAME") ?: ""
        if (username.isEmpty()) {
            val sharedPref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
            username = sharedPref.getString("USERNAME", "") ?: ""
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để tiếp tục!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        rvAddress = findViewById(R.id.rvAddressList)
        tvEmpty = findViewById(R.id.tvEmpty) // Đảm bảo ông có ID này trong XML
        ivBack = findViewById(R.id.ivBackAddressList)
        btnAddAddress = findViewById(R.id.btnAddAddress)
    }

    private fun setupRecyclerView() {
        rvAddress.layoutManager = LinearLayoutManager(this)
        adapter = AddressAdapter(addressList) { selectedAddress ->
            // Khi người dùng chọn một địa chỉ để thanh toán
            val intent = Intent()
            intent.putExtra("SELECTED_ADDRESS", selectedAddress)
            setResult(RESULT_OK, intent)
            finish()
        }
        rvAddress.adapter = adapter
    }

    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }

        btnAddAddress.setOnClickListener {
            val intent = Intent(this, AddAddressActivity::class.java)
            intent.putExtra("USERNAME", username)
            startActivity(intent)
        }
    }

    private fun loadAddresses() {
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("Addresses").child(username)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                addressList.clear()
                for (data in snapshot.children) {
                    val addr = data.getValue(Address::class.java)
                    if (addr != null) {
                        addressList.add(addr)
                    }
                }

                // Địa chỉ mặc định lên đầu
                addressList.sortByDescending { it.isDefault }

                // Xử lý hiển thị thông báo trống
                if (addressList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvAddress.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvAddress.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddressListActivity, "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ==========================================
    // ADAPTER & VIEWHOLDER
    // ==========================================
    inner class AddressAdapter(
        private val list: List<Address>,
        private val onSelect: (Address) -> Unit
    ) : RecyclerView.Adapter<AddressAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNamePhone = v.findViewById<TextView>(R.id.tvNamePhone)
            val tvPhoneDisplay = v.findViewById<TextView>(R.id.tvPhoneDisplay)
            val tvFullAddress = v.findViewById<TextView>(R.id.tvFullAddress)
            val tagDefault = v.findViewById<TextView>(R.id.tagDefault)
            val btnSetDefault = v.findViewById<TextView>(R.id.btnSetDefault)
            val ivEdit = v.findViewById<ImageView>(R.id.ivEditAddress)
            val ivDelete = v.findViewById<ImageView>(R.id.ivDeleteAddress)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val addr = list[position]

            // Hiển thị thông tin
            holder.tvNamePhone.text = addr.receiverName
            holder.tvPhoneDisplay.text = addr.phone
            holder.tvFullAddress.text = "${addr.detail}, ${addr.ward}, ${addr.district}, ${addr.cityProvince}"

            // Trạng thái mặc định
            if (addr.isDefault) {
                holder.tagDefault.visibility = View.VISIBLE
                holder.btnSetDefault.visibility = View.GONE
            } else {
                holder.tagDefault.visibility = View.GONE
                holder.btnSetDefault.visibility = View.VISIBLE
            }

            // Đổi địa chỉ mặc định (Tối ưu dùng updateChildren)
            holder.btnSetDefault.setOnClickListener {
                val ref = FirebaseDatabase.getInstance(dbUrl).getReference("Addresses").child(username)
                ref.get().addOnSuccessListener { snapshot ->
                    val updates = mutableMapOf<String, Any?>()
                    for (child in snapshot.children) {
                        val key = child.key ?: continue
                        updates["$key/isDefault"] = (key == addr.id)
                    }
                    ref.updateChildren(updates).addOnSuccessListener {
                        Toast.makeText(this@AddressListActivity, "Đã đặt làm mặc định!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Xóa địa chỉ (Có hộp thoại xác nhận)
            holder.ivDelete.setOnClickListener {
                AlertDialog.Builder(this@AddressListActivity)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa địa chỉ này không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        FirebaseDatabase.getInstance(dbUrl).getReference("Addresses")
                            .child(username).child(addr.id).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(this@AddressListActivity, "Đã xóa địa chỉ!", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }

            // Sửa địa chỉ (Truyền object Address đầy đủ kèm GHN IDs)
            holder.ivEdit.setOnClickListener {
                val intent = Intent(this@AddressListActivity, AddAddressActivity::class.java)
                intent.putExtra("USERNAME", username)
                intent.putExtra("EDIT_ADDRESS", addr)
                startActivity(intent)
            }

            // Chọn địa chỉ này
            holder.itemView.setOnClickListener {
                onSelect(addr)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}