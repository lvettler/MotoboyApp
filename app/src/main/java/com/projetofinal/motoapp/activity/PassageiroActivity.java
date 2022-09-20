package com.projetofinal.motoapp.activity;

import android.Manifest;
import android.app.ActionBar;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationRequestCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.projetofinal.motoapp.R;
import com.projetofinal.motoapp.config.ConfiguracaoFirebase;
import com.projetofinal.motoapp.databinding.ActivityMainBinding;
import com.projetofinal.motoapp.databinding.ActivityPassageiroBinding;
import com.projetofinal.motoapp.helper.UsuarioFirebase;
import com.projetofinal.motoapp.model.Destino;
import com.projetofinal.motoapp.model.Requisicao;
import com.projetofinal.motoapp.model.Usuario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class PassageiroActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    /*
    * Lat/lon destino: -23.556407, -46.662365 (av. paulista, 2439)
    * Lat/lon passageiro: -23.562791, -46.654668
    * Lat/lon Motorista (a caminho):
    *   inicial: -23.563196, -46.6506007
    *   intermediaria: -23.564801, -46.652196
    *   final: -23.563136, -46.654247
    * */

    //Componentes
    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarMoto;

    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private boolean motoChamado = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);

        inicializarComponentes();

        //Adiciona listener para status da requisição
        verificaStatusRequisicao();

    }

    private void verificaStatusRequisicao(){

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo( usuarioLogado.getId() );

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for ( DataSnapshot ds: snapshot.getChildren() ) {
                    requisicao = ds.getValue(Requisicao.class);
                    lista.add(requisicao);
                }

                Collections.reverse(lista);
                if( lista != null && lista.size()>0 ){
                    requisicao = lista.get(0);

                    switch (requisicao.getStatus()) {
                        case Requisicao.STATUS_AGUARDANDO:
                            linearLayoutDestino.setVisibility(View.GONE);
                            buttonChamarMoto.setText("Cancelar Motoboy");
                            motoChamado = true;
                            break;
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

/**
 * Manipulates the map once available.
 * This callback is triggered when the map is ready to be used.
 * This is where we can add markers or lines, add listeners or move the camera. In this case,
 * we just add a marker near Sydney, Australia.
 * If Google Play services is not installed on the device, the user will be prompted to install
 * it inside the SupportMapFragment. This method will only be triggered once the user has
 * installed Google Play services and returned to the app.
 */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar localização do usuário
        recuperarLocalizacaoUsuario();

    }

    public void chamarMotoboy(View view){

        if( !motoChamado ) { //moto não foi chamado

            String enderecoDestino = editDestino.getText().toString();

            if( !enderecoDestino.equals("") || enderecoDestino != null){

                Address addressDestino = recuperarEndereco( enderecoDestino );
                if( addressDestino != null ){

                    /* foi estudado a biblioteca Address /
                     * https://developer.android.com/reference/android/location/Address
                     * */
                    Destino destino = new Destino();
                    destino.setCidade(addressDestino.getAdminArea());
                    destino.setCep(addressDestino.getPostalCode());
                    destino.setBairro(addressDestino.getSubLocality());
                    destino.setRua(addressDestino.getThoroughfare());
                    destino.setNumero(addressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append( "Cidade: " + destino.getCidade() );
                    mensagem.append( "\nRua: " + destino.getRua() );
                    mensagem.append( "\nBairro: " + destino.getBairro() );
                    mensagem.append( "\nNúmero: " + destino.getNumero() );
                    mensagem.append( "\nCep: " + destino.getCep() );

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme seu endereço!")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    //salvar requisição
                                    salvarRequisicao(destino);
                                    motoChamado = true;

                                }

                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }else{
                Toast.makeText(this,
                        "Informe o endereço de destino!",
                        Toast.LENGTH_SHORT).show();
            }

        }else {
            //Cancelar a requisição

            motoChamado = false;

        }


    }

    private void salvarRequisicao(Destino destino){

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino( destino );

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude( String.valueOf( localPassageiro.latitude ) );
        usuarioPassageiro.setLongitude( String.valueOf( localPassageiro.longitude ) );

        requisicao.setPassageiro( usuarioPassageiro );
        requisicao.setStatus( Requisicao.STATUS_AGUARDANDO );
        requisicao.salvar();

        linearLayoutDestino.setVisibility( View.GONE );
        buttonChamarMoto.setText( "Cancelar Motoboy" );

    }

    private Address recuperarEndereco(String endereco){

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);
            if( listaEnderecos != null && listaEnderecos.size() > 0){ //verifica se tem 1 endereço na lista de endereços
                Address address = listaEnderecos.get(0);

                return address;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged( Location location ) {

                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                //incluindo o marcador no mapa
                localPassageiro = new LatLng(latitude, longitude);

                mMap.clear();
                mMap.addMarker(
                        new MarkerOptions()
                                .position(localPassageiro)
                                .title("Meu local")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
                );
                mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(localPassageiro, 20)
                );
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //Solicitar atualização de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menuSair :
                autenticacao.signOut();
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void inicializarComponentes(){

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle( "Iniciar uma viagem" );
        setSupportActionBar(toolbar);

        //inicializar componentes
        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarMoto = findViewById(R.id.buttonChamarMoto);

        //configurações iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

}
