package com.projetofinal.motoapp.helper;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.projetofinal.motoapp.activity.PassageiroActivity;
import com.projetofinal.motoapp.activity.RequisicoesActivity;
import com.projetofinal.motoapp.config.ConfiguracaoFirebase;
import com.projetofinal.motoapp.model.Usuario;

public class UsuarioFirebase {

    public static FirebaseUser getUsuarioAtual(){
        FirebaseAuth usuario = ConfiguracaoFirebase.getFirebaseAutenticacao();
        return usuario.getCurrentUser();
    }

    public static Usuario getDadosUsuarioLogado(){

        FirebaseUser firebaseUser = getUsuarioAtual();

        Usuario usuario = new Usuario();
        usuario.setId( firebaseUser.getUid() );
        usuario.setEmail( firebaseUser.getEmail() );
        usuario.setNome( firebaseUser.getDisplayName() );

        return usuario;
    }

    public static boolean atualizarNomeUsuario(String nome){

        try {

            FirebaseUser user = getUsuarioAtual();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName( nome )
                    .build();
            user.updateProfile( profile ).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if( !task.isSuccessful() ){
                        Log.d("Perfil", "Erro ao atualizar nome de perfil.");
                    }
                }
            });

            return true;

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    public static void redirecionaUsuarioLogado( Activity activity ){

        FirebaseUser user = getUsuarioAtual();
        if( user != null ){
            DatabaseReference usuarioRef = ConfiguracaoFirebase.getFirebaseDatabase()
                    .child("usuarios")
                    .child(getIdentificadorUsuario());
            usuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    Usuario usuario = snapshot.getValue( Usuario.class );

                    String tipoUsuario = usuario.getTipo();
                    if( tipoUsuario.equals("M")){
                        Intent i = new Intent(activity, RequisicoesActivity.class);
                        activity.startActivity(i);
                    }else {
                        Intent i = new Intent(activity, PassageiroActivity.class);
                        activity.startActivity(i);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }

    }

    public static String getIdentificadorUsuario(){
        return getUsuarioAtual().getUid();
    }


}
