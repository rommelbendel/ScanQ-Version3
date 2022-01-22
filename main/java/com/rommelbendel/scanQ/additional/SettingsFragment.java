package com.rommelbendel.scanQ.additional;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.rommelbendel.scanQ.R;
import com.yuvraj.livesmashbar.enums.BarStyle;
import com.yuvraj.livesmashbar.enums.GravityView;
import com.yuvraj.livesmashbar.view.LiveSmashBar;

public class SettingsFragment extends PreferenceFragmentCompat {

    EditTextPreference name, keyword;
    SwitchPreferenceCompat hello, lowerCase, cats, quizSounds, visuallyImpaired, useKeyword;
    DropDownPreference accent;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        TinyDB tbSettings = new TinyDB(getContext());
        SharedPreferences pref = requireContext().getSharedPreferences("settings",0);
        SharedPreferences.Editor edit = pref.edit();

        name = findPreference("signature");
        assert name != null;
        name.setText(pref.getString("username", "hola"));
        hello = findPreference("personal");
        cats = findPreference("cats");
        lowerCase = findPreference("lowercase");
        quizSounds = findPreference("quiz_sounds");
        accent = findPreference("accent");
        visuallyImpaired = findPreference("visually_impaired");
        useKeyword = findPreference("use_keyword");
        keyword = findPreference("keyword");

        name.setOnPreferenceChangeListener((preference, newValue) -> {
            if(newValue.toString().trim().length() != 0) {
                edit.putString("username", newValue.toString().trim());
                edit.apply();
                name.setText(newValue.toString().trim());
            } else {
                new LiveSmashBar.Builder(SettingsFragment.this.requireActivity())
                        .title("Bitte gib deinen Namen ein")
                        .titleColor(Color.WHITE)
                        .backgroundColor(Color.parseColor("#541111"))
                        .gravity(GravityView.BOTTOM)
                        .primaryActionText("Ok")
                        .primaryActionEventListener(LiveSmashBar::dismiss)
                        .duration(3000)
                        .show();
            }
            return true;
        });

       hello.setOnPreferenceChangeListener((preference, newValue) -> {
            edit.putBoolean("personal", hello.isChecked());
            edit.apply();
            return true;
        });

        cats.setOnPreferenceChangeListener((preference, newValue) -> {
            edit.putBoolean("cats", cats.isChecked());
            edit.apply();

            /*new LiveSmashBar.Builder(requireActivity())
                    .title("Diese Einstellung erfordert einen Neustart.")
                    .titleColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                    .titleSizeInSp(19)
                    .backgroundColor(Color.WHITE)
                    .setBarStyle(BarStyle.DIALOG)
                    .positiveActionText("abbrechen")
                    .positiveActionTextSizeInSp(16)
                    .positiveActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                    .positiveActionEventListener(LiveSmashBar::dismiss)
                    .negativeActionText("ok")
                    .negativeActionTextSizeInSp(16)
                    .negativeActionTextColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                    .negativeActionEventListener(liveSmashBar -> {
                        requireActivity().finishAffinity();
                        System.exit(0);
                    })
                    .showOverlay()
                    .overlayBlockable()
                    .duration(20000)
                    .show();*/
            return true;
        });

        lowerCase.setOnPreferenceChangeListener((preference, newValue) -> {
            tbSettings.putBoolean("lowercase", lowerCase.isChecked());
            return true;
        });

        quizSounds.setOnPreferenceChangeListener((preference, newValue) -> {
            edit.putBoolean("quiz_sounds", quizSounds.isChecked());
            edit.apply();
            return true;
        });

        accent.setOnPreferenceChangeListener(((preference, newValue) -> {
            tbSettings.putString("accent", newValue.toString().trim());
            return true;
        }));

        visuallyImpaired.setOnPreferenceChangeListener(((preference, newValue) -> {
            new LiveSmashBar.Builder(requireActivity())
                    .title("Der Blindenmodus erfordert einen Neustart.")
                    .titleColor(Color.WHITE)
                    .titleSizeInSp(19)
                    .backgroundColor(Color.parseColor("#2f3030"))
                    .setBarStyle(BarStyle.DIALOG)
                    .positiveActionText("abbrechen")
                    .positiveActionTextSizeInSp(16)
                    .positiveActionTextColor(Color.WHITE)
                    .positiveActionEventListener(l -> {
                        visuallyImpaired.setChecked(false);
                        l.dismiss();
                    })
                    .negativeActionTextSizeInSp(16)
                    .negativeActionTextColor(Color.WHITE)
                    .negativeActionText("OK")
                    .negativeActionEventListener(liveSmashBar -> {
                        tbSettings.putBoolean("visually_impaired", (Boolean) newValue);
                        useKeyword.setEnabled((Boolean) newValue);
                        if (useKeyword.isChecked()) keyword.setEnabled((Boolean) newValue);

                        requireActivity().finishAffinity();
                        System.exit(0);
                    })
                    .showOverlay()
                    .overlayBlockable()
                    .duration(20000)
                    .show();
            return true;
        }));

        //TODO: erst Einstellungen ändern und dann Bildenmodus aktivieren für Neustart mit Warnung!
        if (visuallyImpaired.isChecked()) useKeyword.setEnabled(true);

        useKeyword.setOnPreferenceChangeListener(((preference, newValue) -> {
            tbSettings.putBoolean("use_keyword", (Boolean) newValue);
            keyword.setEnabled((Boolean) newValue);
            return true;
        }));

        if (useKeyword.isChecked()) keyword.setEnabled(true);

        keyword.setOnPreferenceChangeListener(((preference, newValue) -> {
            final String newKeyWord = String.valueOf(newValue);
            if (!newKeyWord.isEmpty() && newKeyWord.split(" ").length == 1
                    && newKeyWord.matches("[a-zA-ZäÄüÜöÖ]+")) {
                tbSettings.putString("keyword", String.valueOf(newValue));
                return true;
            } else {
                LiveSmashBar.Builder warning = new LiveSmashBar.Builder(getActivity());
                warning.title("unzulässiges Aktivierungswort");
                warning.description("Es sind nur einzelnde Wörter aus Buchstaben zulässig.");
                warning.titleColor(Color.WHITE);
                warning.backgroundColor(Color.parseColor("#541111"));
                warning.gravity(GravityView.BOTTOM);
                warning.dismissOnTapOutside();
                warning.showOverlay();
                warning.overlayBlockable();
                warning.duration(3000);
                warning.show();
                return false;
            }
        }));
    }
}