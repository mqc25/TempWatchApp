package ucla.invistahealth.watch_app.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 111;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            requireActivity().registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    return;
                } else {
                    checkPermission();
                }
            });

    private void checkPermission(){
        ArrayList<String> permissionLists = new ArrayList<>();
        permissionLists.add(Manifest.permission.WAKE_LOCK);
        permissionLists.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionLists.add(Manifest.permission.BLUETOOTH);
        permissionLists.add(Manifest.permission.BLUETOOTH_ADMIN);
        permissionLists.add(Manifest.permission.INTERNET);
        permissionLists.add(Manifest.permission.READ_PHONE_STATE);
        permissionLists.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionLists.add(Manifest.permission.BODY_SENSORS);
        permissionLists.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissionLists.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissionLists.add(Manifest.permission.CHANGE_WIFI_STATE);
        permissionLists.add(Manifest.permission.CHANGE_NETWORK_STATE);

        ArrayList<String> notGrantedPermission = new ArrayList<>();

        for(String permission: permissionLists){
            int result = ContextCompat.checkSelfPermission(requireContext(),permission);
            if(result != PackageManager.PERMISSION_GRANTED){
                notGrantedPermission.add(permission);
            }
        }

        if(!notGrantedPermission.isEmpty()){
            requestPermissions((String[]) permissionLists.toArray(), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow
                // in your app.
            } else {
                checkPermission();
            }
        }
    }
}
