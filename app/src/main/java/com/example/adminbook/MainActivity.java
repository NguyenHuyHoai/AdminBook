package com.example.adminbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.bumptech.glide.Glide;
import com.example.adminbook.account.changepass.ChangePassFragment;
import com.example.adminbook.account.information.InformationFragment;
import com.example.adminbook.databinding.ActivityMainBinding;
import com.example.adminbook.databinding.LayoutHeaderNavBinding;
import com.example.adminbook.listbooks.ListBooksFragment;
import com.example.adminbook.listgenres.ListGenresFragment;
import com.example.adminbook.listusers.ItemUsers;
import com.example.adminbook.listusers.ListUsersFragment;
import com.example.adminbook.system.Login;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    ActivityMainBinding binding;
    LayoutHeaderNavBinding headerBinding;
    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    CollectionReference usersCollection = firebaseFirestore.collection("Users");
    private ItemUsers itemUsers;
    private  static final int FRAGMENT_LISTBOOKS = 0;
    private  static final int FRAGMENT_LISTGENRES = 1;
    private  static final int FRAGMENT_LISTCHAPTERS = 2;
    private  static final int FRAGMENT_LISTUSERS = 3;
    private  static final int FRAGMENT_INFORMATION = 4;
    private  static final int FRAGMENT_CHANGEPASS = 5;
    private  static final int FRAGMENT_SIGNOUT = 6;
    private int mCurrentFragment = FRAGMENT_LISTBOOKS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        if (intent != null) {
            String itemUsersJson = intent.getStringExtra("itemUsersJson");
            if (itemUsersJson != null) {
                Gson gson = new Gson();
                itemUsers = gson.fromJson(itemUsersJson, ItemUsers.class);
                String userDocumentId = itemUsers.getUsersId();
                loadPersonalInformationFromFirestore(userDocumentId);
            }
        }

        setSupportActionBar(binding.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout,
                binding.toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);
        replaceFragment(new ListBooksFragment());
        binding.navView.getMenu().findItem(R.id.nav_book).setCheckable(true);
        getSupportActionBar().setTitle("List Book");
    }

    private void loadPersonalInformationFromFirestore( String userDocumentId) {

        usersCollection.document(userDocumentId)
                        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                                if (error != null) {
                                    // Xử lý lỗi nếu có
                                    return;
                                }
                                if (value != null && value.exists()) {
                                    itemUsers = value.toObject(ItemUsers.class);
                                    updateNavigationMenu(itemUsers);
                                    populateHeaderLayout(itemUsers);
                                }
                            }
                        });
    }

    //Phân quyền
    private void updateNavigationMenu(ItemUsers itemUsers) {
        Menu navMenu = binding.navView.getMenu();
        MenuItem navUserItem = navMenu.findItem(R.id.nav_user);

        if (itemUsers.getUsersName().equals("huyhoai")) {
            navUserItem.setVisible(true);
        } else {
            navUserItem.setVisible(false);
        }
    }
    //Chọn Item trong Menu
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.nav_book)
        {
            if(mCurrentFragment != FRAGMENT_LISTBOOKS)
            {
                replaceFragment(new ListBooksFragment());
                mCurrentFragment = FRAGMENT_LISTBOOKS;
                getSupportActionBar().setTitle("List Books");
            }
        } else if (id == R.id.nav_genres) {
            if(mCurrentFragment != FRAGMENT_LISTGENRES)
            {
                replaceFragment(new ListGenresFragment());
                mCurrentFragment = FRAGMENT_LISTGENRES;
                getSupportActionBar().setTitle("List Genres");
            }
        }else if (id == R.id.nav_user) {
            if(mCurrentFragment != FRAGMENT_LISTUSERS)
            {
                replaceFragment(new ListUsersFragment());
                mCurrentFragment = FRAGMENT_LISTUSERS;
                getSupportActionBar().setTitle("List Users");
            }
        }else if (id == R.id.nav_myproject) {
            if(mCurrentFragment != FRAGMENT_INFORMATION)
            {

                Gson gson = new Gson();
                String itemUsersJson = gson.toJson(itemUsers);
                InformationFragment informationFragment = new InformationFragment();
                Bundle args = new Bundle();
                args.putString("itemUsersJson", itemUsersJson);
                informationFragment.setArguments(args);

                replaceFragment(informationFragment);
                mCurrentFragment = FRAGMENT_INFORMATION;
                getSupportActionBar().setTitle("Personal Information");
            }
        }else if (id == R.id.nav_changePW) {
            if(mCurrentFragment != FRAGMENT_CHANGEPASS)
            {
                Gson gson = new Gson();
                String itemUsersJson = gson.toJson(itemUsers);
                ChangePassFragment changePassFragment = new ChangePassFragment();
                Bundle args = new Bundle();
                args.putString("itemUsersJson", itemUsersJson);
                changePassFragment.setArguments(args);

                replaceFragment(changePassFragment);
                mCurrentFragment = FRAGMENT_CHANGEPASS;
                getSupportActionBar().setTitle("Change Password");
            }
        }else if (id == R.id.nav_signout) {
            if(mCurrentFragment != FRAGMENT_SIGNOUT)
            {
                showLogoutConfirmationDialog();
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.icon_not_verified)
            .setTitle("Xác nhận đăng xuất")
            .setMessage("Bạn có muốn đăng xuất không?")
            .setPositiveButton("Xác nhận", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
                finish();
            }
        })
            .setNegativeButton("Đóng", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }
    private void replaceFragment(Fragment fragment){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }
    private void populateHeaderLayout(ItemUsers itemUsers) {
        headerBinding = LayoutHeaderNavBinding.bind(binding.navView.getHeaderView(0));

        headerBinding.tvName.setText(itemUsers.getUsersName());
        headerBinding.tvNameEmail.setText(itemUsers.getEmail());
        if (!isFinishing()) {
            Glide.with(this)
                    .load(itemUsers.getAvatar())
                    .placeholder(R.drawable.icon_load) // Hình thay thế trong quá trình tải
                    .error(R.drawable.defaultavatar) // Hình để hiển thị nếu tải thất bại
                    .into(headerBinding.profileImage);
        }
    }
}