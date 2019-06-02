package br.com.ricardo347;



import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.support.annotation.NonNull;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView locationTextView;

    private double lat, lon;
    private RecyclerViewAdapter adapter;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int REQUEST_CODE_GPS = 1001;
    private List<Localizacao> localizacoes;
    private RecyclerView recyclerView;
    private Button botao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // alimentando a lista de ultimas localidades persistida no banco dados.
        localizacoes = buscaLocalizacao();
        adapter = new RecyclerViewAdapter(localizacoes);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);



        locationManager = (LocationManager)  getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lat = location.getLatitude();
                lon = location.getLongitude();

 // ###############################################################################################################
 // Implementação da solicitação do exercício, guardar os ultimos 50

                //SE JA TEM 50, DELETA O PRIMEIRO E INSERE
                if(localizacoes.size() == 50){

                    deletaPrimeiraDb();

                    localizacoes.remove(0);
                    localizacoes.add(new Localizacao(lat, lon));

                    insereCoordDb(lat, lon);
                    adapter.notifyDataSetChanged();
                }else
                {
                    insereCoordDb(lat, lon);
                    localizacoes.add(new Localizacao(lat, lon));
                    adapter.notifyDataSetChanged();
                }

            }
//########################################################################################################################

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
    }
// ###########################################################################################################################
// metodos de manipulação dos dados no banco

    //INSERE AS COODENADAS NO BANCO
    public void insereCoordDb(double lat, double lon){
        dbHelper dbh = new dbHelper(getApplicationContext());
        SQLiteDatabase db = dbh.getReadableDatabase();
        String sql = String.format("INSERT INTO COORDENADAS (LAT, LON)VALUES (%s,%s)", lat, lon);
        db.execSQL(sql);
        db.close();
    }

    //DELETA COORDENADAS NO BANCO
    public void deletaCoordDb(int pos){
        dbHelper dbh = new dbHelper(getApplicationContext());
        SQLiteDatabase db = dbh.getReadableDatabase();
        String sql = String.format("DELETE FROM COORDENADAS WHERE POSITION = %s",pos);
    }
    //USADO PARA DELETAR O PRIMEIRO DA FILA, PARA QUE SEMPRE SEJA EXIBIDO OS 50 ULTIMOS

    public void deletaPrimeiraDb (){
        dbHelper dbh = new dbHelper(this);
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor cursor =  db.rawQuery("SELECT MIN(POSITION) FROM COORDENADAS", null);
        int pos = 0;

        if(cursor != null && cursor.moveToNext()){
            pos = cursor.getInt(0);
        }
        String sql = String.format("DELETE FROM COORDENADAS WHERE POSITION = %s", pos);
        db.execSQL(sql);
    }

    public List<Localizacao> buscaLocalizacao(){
        List<Localizacao> localizacoes = new ArrayList<>();
        dbHelper dbh = new dbHelper(this);
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM COORDENADAS",null);

        if(cursor != null){
            while(cursor.moveToNext()){
                localizacoes.add(new Localizacao(
                        cursor.getDouble(0),
                        cursor.getDouble(1)
                ));
            }
        }
        return localizacoes;
    }

    //#################################################################################################################

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            locationManager.
                    requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            2000,
                            5,
                            locationListener
                    );
        }
        else{
            ActivityCompat.requestPermissions(
                    this,
                    new String []{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_GPS
            );
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_GPS){
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            2000,
                            5,
                            locationListener
                    );
                }
            }
            else{
                Toast.makeText(this, "erro", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class MeuViewHolder extends RecyclerView.ViewHolder{

        TextView latitudeTextView;
        TextView longitudeTextView;
        TextView posTextView;

        public MeuViewHolder (View raiz){
            super (raiz);
            latitudeTextView = raiz.findViewById(R.id.latitudeTextView);
            longitudeTextView = raiz.findViewById(R.id.longitudeTextView);
            posTextView = raiz.findViewById(R.id.posTextView);
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter <MeuViewHolder>{
        List<Localizacao> localizacoes;

        public RecyclerViewAdapter (List <Localizacao> localizacoes){
            this.localizacoes = localizacoes;
        }

        @NonNull
        @Override
        public MeuViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            Context context = viewGroup.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View raiz = inflater.inflate(R.layout.list_item, viewGroup, false);
            return new MeuViewHolder(raiz);
        }

        @Override
        public void onBindViewHolder(@NonNull MeuViewHolder meuViewHolder, int i) {
            Localizacao localizacao = localizacoes.get(i);
            meuViewHolder.latitudeTextView.setText(Double.toString(localizacao.latitude));
            meuViewHolder.longitudeTextView.setText(Double.toString(localizacao.longitude));
            meuViewHolder.posTextView.setText(String.valueOf(i));
        }

        @Override
        public int getItemCount() {
            return localizacoes.size();
        }
    }


    private class Localizacao{
        double latitude;
        double longitude;


        public Localizacao (double latitude, double longitude){
            this.latitude = latitude;
            this.longitude = longitude;

        }
    }
//############################################################################################################################
    //clase fabrica de conexão com o banco
    private class dbHelper extends SQLiteOpenHelper{
        private static final String DB_NAME = "chats.db";
        private static final int DB_VERSION = 1;

        dbHelper(Context context){

            super(context, DB_NAME,null,DB_VERSION);
        }


// a tabela de coordenadas, tem um campo position como autoincremento, pois na logica estabelecida
    // quando chega a 50 ele exclui o primeiro, fica muito mais fácil sempre manter 50 itens no banco
    //e excluir o min(position).
        @Override
        public void onCreate(SQLiteDatabase db) {
           db.execSQL("CREATE TABLE COORDENADAS (POSITION INTEGER PRIMARY KEY AUTOINCREMENT, LAT REAL, LON REAL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }


    }


}













