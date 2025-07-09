package vn.edu.fpt.gameproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;

import vn.edu.fpt.gameproject.fragment.BoardListFragment;
import vn.edu.fpt.gameproject.fragment.BoardSelectionFragment;
import vn.edu.fpt.gameproject.fragment.HelpFragment;
import vn.edu.fpt.gameproject.fragment.SettingsFragment;
import vn.edu.fpt.gameproject.fragment.StartGameFragment;
import vn.edu.fpt.gameproject.model.BoardState;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
        navigationView.setCheckedItem(R.id.nav_play);

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        if (savedInstanceState == null) {
            loadFragment(new StartGameFragment());
        }
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_play) {
            fragment = new StartGameFragment();
        } else if (itemId == R.id.nav_board_list) {
            fragment = new BoardListFragment();
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else if (itemId == R.id.nav_help) {
            fragment = new HelpFragment();
        }

        if (fragment != null) {
            loadFragment(fragment);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}