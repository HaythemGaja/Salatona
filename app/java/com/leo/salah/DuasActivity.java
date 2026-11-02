package com.leo.salah;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Complete duas & adhkar: after prayer, morning, and night. */
public class DuasActivity extends BaseActivity {

    private static final String[] CATS = {
            "🤲 After Salah",
            "🌅 Morning adhkar · أذكار الصباح",
            "🌙 Night adhkar · أذكار المساء",
    };

    // {title, arabic, transliteration, english} per category
    private static final String[][][] DUAS = {
        { // ---------- after salah ----------
            {"After every prayer",
                    "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
                    "Allahumma antas-salam wa minkas-salam, tabarakta ya dhal-jalali wal-ikram",
                    "O Allah, You are Peace and from You comes peace. Blessed are You, O Owner of majesty and honor."},
            {"Seeking forgiveness (3x)",
                    "أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ",
                    "Astaghfirullah (3 times)",
                    "I seek Allah's forgiveness."},
            {"Tasbih after prayer",
                    "سُبْحَانَ اللَّهِ (٣٣) ، الْحَمْدُ لِلَّهِ (٣٣) ، اللَّهُ أَكْبَرُ (٣٤)",
                    "SubhanAllah (33x), Alhamdulillah (33x), Allahu Akbar (34x)",
                    "Glory be to Allah, praise be to Allah, Allah is Greatest."},
            {"Ayat al-Kursi",
                    "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ",
                    "Allahu la ilaha illa huwal-hayyul-qayyum… (Quran 2:255)",
                    "Whoever recites it after every prayer, nothing stands between him and Paradise except death."},
            {"The comprehensive dua",
                    "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                    "Rabbana atina fid-dunya hasanah, wa fil-akhirati hasanah, wa qina 'adhaban-nar",
                    "Our Lord, give us good in this world and good in the Hereafter, and shield us from the punishment of the Fire."},
            {"For parents",
                    "رَبِّ اغْفِرْ لِي وَلِوَالِدَيَّ رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
                    "Rabbi-ghfir li wa liwalidayya, rabbi-rhamhuma kama rabbayani saghira",
                    "My Lord, forgive me and my parents, and have mercy on them as they raised me when I was small."},
        },
        { // ---------- morning adhkar ----------
            {"We have entered the morning",
                    "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
                    "Asbahna wa asbahal-mulku lillah, wal-hamdu lillah, la ilaha illallah wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa huwa 'ala kulli shay'in qadir",
                    "We have entered the morning and with it all dominion belongs to Allah. Praise be to Allah; none has the right to be worshipped but Allah alone, without partner."},
            {"By You we enter the morning",
                    "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
                    "Allahumma bika asbahna, wa bika amsayna, wa bika nahya, wa bika namut, wa ilaykan-nushur",
                    "O Allah, by You we enter the morning and by You we enter the evening; by You we live and by You we die, and to You is the resurrection."},
            {"Ayat al-Kursi",
                    "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
                    "Allahu la ilaha illa huwal-hayyul-qayyum… (Quran 2:255, full verse)",
                    "Whoever recites it in the morning is protected until the evening."},
            {"The three Quls (3x each)",
                    "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
                    "Qul huwallahu ahad… (Al-Ikhlas), then Al-Falaq and An-Nas, 3 times each",
                    "Whoever recites the three Quls three times in the morning, they protect him from everything until the evening."},
            {"Beneficial knowledge",
                    "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
                    "Allahumma inni as'aluka 'ilman nafi'an, wa rizqan tayyiban, wa 'amalan mutaqabbal",
                    "O Allah, I ask You for beneficial knowledge, wholesome provision, and accepted deeds."},
            {"Pleased with Allah (3x)",
                    "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
                    "Raditu billahi rabban, wa bil-islami dinan, wa bi Muhammadin sallallahu 'alayhi wa sallam nabiyya (3x)",
                    "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad ﷺ as my Prophet."},
            {"Calling Allah to witness (4x)",
                    "اللَّهُمَّ إِنِّي أَصْبَحْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ",
                    "Allahumma inni asbahtu ush-hiduk, wa ush-hidu hamalata 'arshik, wa mala'ikatak, wa jami'a khalqik, annaka antallah la ilaha illa ant wahdak la sharika lak, wa anna Muhammadan 'abduka wa rasuluk (4x)",
                    "Whoever says it four times in the morning, Allah frees him from the Fire."},
            {"Every blessing is from You",
                    "اللَّهُمَّ مَا أَصْبَحَ بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ",
                    "Allahumma ma asbaha bi min ni'matin aw bi-ahadin min khalqika faminka wahdaka la sharika lak, falakal-hamdu wa lakash-shukr",
                    "O Allah, whatever blessing has come to me or to any of Your creation this morning is from You alone, without partner, so to You is all praise and thanks."},
            {"Allah suffices me (7x)",
                    "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ، عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
                    "Hasbiyallahu la ilaha illa hu, 'alayhi tawakkaltu wa huwa rabbul-'arshil-'azim (7x)",
                    "Allah suffices me; none has the right to be worshipped but He. In Him I place my trust, and He is the Lord of the Mighty Throne."},
            {"O Ever-Living, O Sustainer",
                    "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ",
                    "Ya Hayyu Ya Qayyum birahmatika astaghith, aslih li sha'ni kullah, wa la takilni ila nafsi tarfata 'ayn",
                    "O Ever-Living, O Sustainer, by Your mercy I seek relief; set all my affairs right and do not leave me to myself for even the blink of an eye."},
            {"Upon the fitrah of Islam",
                    "أَصْبَحْنَا عَلَى فِطْرَةِ الْإِسْلَامِ، وَعَلَى كَلِمَةِ الْإِخْلَاصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ",
                    "Asbahna 'ala fitratil-islam, wa 'ala kalimatil-ikhlas, wa 'ala dini nabiyyina Muhammadin sallallahu 'alayhi wa sallam, wa 'ala millati abina Ibrahima hanifan musliman wa ma kana minal-mushrikin",
                    "We have entered the morning upon the natural religion of Islam, the word of sincerity, the religion of our Prophet Muhammad ﷺ, and the way of our father Abraham, upright and Muslim."},
            {"Glory and praise (100x)",
                    "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                    "SubhanAllahi wa bihamdih (100x)",
                    "Glory be to Allah and praise Him — whoever says it 100 times, his sins are forgiven though they be like the foam of the sea."},
            {"As many as His creation (3x)",
                    "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
                    "SubhanAllahi wa bihamdihi 'adada khalqih, wa rida nafsih, wa zinata 'arshih, wa midada kalimatih (3x)",
                    "Glory be to Allah and praise Him, as many times as the number of His creation, as much as pleases Him, as much as the weight of His Throne, and as much as the ink of His words."},
            {"Health and refuge (3x)",
                    "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَهَ إِلَّا أَنْتَ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَهَ إِلَّا أَنْتَ",
                    "Allahumma 'afini fi badani, Allahumma 'afini fi sam'i, Allahumma 'afini fi basari, la ilaha illa ant, Allahumma inni a'udhu bika minal-kufri wal-faqr, Allahumma inni a'udhu bika min 'adhabil-qabr, la ilaha illa ant (3x)",
                    "O Allah, grant me health in my body, hearing and sight; none has the right to be worshipped but You. I seek refuge in You from disbelief, poverty, and the punishment of the grave."},
            {"The master of seeking forgiveness",
                    "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
                    "Allahumma anta rabbi la ilaha illa ant, khalaqtani wa ana 'abduk, wa ana 'ala 'ahdika wa wa'dika mastata't, a'udhu bika min sharri ma sana't, abu'u laka bini'matika 'alayy, wa abu'u bidhanbi faghfir li, fa innahu la yaghfirudh-dhunuba illa ant",
                    "The master of seeking forgiveness — whoever says it in the morning with certainty and dies that day enters Paradise."},
            {"Nothing can harm (3x)",
                    "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
                    "Bismillahilladhi la yadurru ma'a ismihi shay'un fil-ardi wa la fis-sama', wa huwas-sami'ul-'alim (3x)",
                    "In the name of Allah, with whose name nothing on earth or in heaven can cause harm; He is the All-Hearing, All-Knowing."},
            {"Creator of the heavens and earth",
                    "اللَّهُمَّ فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي، وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ",
                    "Allahumma fatiras-samawati wal-ard, 'alimal-ghaybi wash-shahadah, rabba kulli shay'in wa malikah, ash-hadu alla ilaha illa ant, a'udhu bika min sharri nafsi, wa min sharrish-shaytani wa shirkih",
                    "O Allah, Creator of the heavens and earth, Knower of the unseen and the seen, Lord and Sovereign of all things, I seek refuge in You from the evil of my soul and from the evil of Satan and his traps."},
            {"Pardon and well-being",
                    "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
                    "Allahumma inni as'alukal-'afwa wal-'afiyata fid-dunya wal-akhirah",
                    "O Allah, I ask You for pardon and well-being in this world and the Hereafter."},
        },
        { // ---------- night adhkar ----------
            {"We have entered the evening",
                    "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
                    "Amsayna wa amsal-mulku lillah, wal-hamdu lillah, la ilaha illallah wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa huwa 'ala kulli shay'in qadir",
                    "We have entered the evening and with it all dominion belongs to Allah. Praise be to Allah; none has the right to be worshipped but Allah alone, without partner."},
            {"By You we enter the evening",
                    "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ",
                    "Allahumma bika amsayna, wa bika asbahna, wa bika nahya, wa bika namut, wa ilaykal-masir",
                    "O Allah, by You we enter the evening and by You we enter the morning; by You we live and by You we die, and to You is the final return."},
            {"Ayat al-Kursi at night",
                    "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
                    "Allahu la ilaha illa huwal-hayyul-qayyum… (Quran 2:255, full verse)",
                    "Whoever recites it at night is protected by Allah and no devil comes near him until morning."},
            {"Last two verses of Al-Baqarah",
                    "آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ وَالْمُؤْمِنُونَ ۚ كُلٌّ آمَنَ بِاللَّهِ وَمَلَائِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ لَا نُفَرِّقُ بَيْنَ أَحَدٍ مِّن رُّسُلِهِ ۚ وَقَالُوا سَمِعْنَا وَأَطَعْنَا ۖ غُفْرَانَكَ رَبَّنَا وَإِلَيْكَ الْمَصِيرُ",
                    "Amanar-rasulu bima unzila ilayhi mir-rabbihi wal-mu'minun… (Quran 2:285-286)",
                    "Whoever recites the last two verses of Al-Baqarah at night, they suffice him."},
            {"The three Quls (3x each)",
                    "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
                    "Qul huwallahu ahad… (Al-Ikhlas), then Al-Falaq and An-Nas, 3 times each",
                    "Whoever recites the three Quls three times in the evening, they protect him from everything until the morning."},
            {"Calling Allah to witness (4x)",
                    "اللَّهُمَّ إِنِّي أَمْسَيْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلَائِكَتَكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَهَ إِلَّا أَنْتَ وَحْدَكَ لَا شَرِيكَ لَكَ، وَأَنَّ مُحَمَّدًا عَبْدُكَ وَرَسُولُكَ",
                    "Allahumma inni amsaytu ush-hiduk, wa ush-hidu hamalata 'arshik, wa mala'ikatak, wa jami'a khalqik, annaka antallah la ilaha illa ant wahdak la sharika lak, wa anna Muhammadan 'abduka wa rasuluk (4x)",
                    "Whoever says it four times in the evening, Allah frees him from the Fire."},
            {"Every blessing is from You",
                    "اللَّهُمَّ مَا أَمْسَى بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لَا شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ",
                    "Allahumma ma amsa bi min ni'matin aw bi-ahadin min khalqika faminka wahdaka la sharika lak, falakal-hamdu wa lakash-shukr",
                    "O Allah, whatever blessing has come to me or to any of Your creation this evening is from You alone, without partner, so to You is all praise and thanks."},
            {"Allah suffices me (7x)",
                    "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ، عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
                    "Hasbiyallahu la ilaha illa hu, 'alayhi tawakkaltu wa huwa rabbul-'arshil-'azim (7x)",
                    "Allah suffices me; none has the right to be worshipped but He. In Him I place my trust, and He is the Lord of the Mighty Throne."},
            {"O Ever-Living, O Sustainer",
                    "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ",
                    "Ya Hayyu Ya Qayyum birahmatika astaghith, aslih li sha'ni kullah, wa la takilni ila nafsi tarfata 'ayn",
                    "O Ever-Living, O Sustainer, by Your mercy I seek relief; set all my affairs right and do not leave me to myself for even the blink of an eye."},
            {"Upon the fitrah of Islam",
                    "أَمْسَيْنَا عَلَى فِطْرَةِ الْإِسْلَامِ، وَعَلَى كَلِمَةِ الْإِخْلَاصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ",
                    "Amsayna 'ala fitratil-islam, wa 'ala kalimatil-ikhlas, wa 'ala dini nabiyyina Muhammadin sallallahu 'alayhi wa sallam, wa 'ala millati abina Ibrahima hanifan musliman wa ma kana minal-mushrikin",
                    "We have entered the evening upon the natural religion of Islam, the word of sincerity, the religion of our Prophet Muhammad ﷺ, and the way of our father Abraham, upright and Muslim."},
            {"As many as His creation (3x)",
                    "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
                    "SubhanAllahi wa bihamdihi 'adada khalqih, wa rida nafsih, wa zinata 'arshih, wa midada kalimatih (3x)",
                    "Glory be to Allah and praise Him, as many times as the number of His creation, as much as pleases Him, as much as the weight of His Throne, and as much as the ink of His words."},
            {"Health and refuge (3x)",
                    "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَهَ إِلَّا أَنْتَ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَهَ إِلَّا أَنْتَ",
                    "Allahumma 'afini fi badani, Allahumma 'afini fi sam'i, Allahumma 'afini fi basari, la ilaha illa ant, Allahumma inni a'udhu bika minal-kufri wal-faqr, Allahumma inni a'udhu bika min 'adhabil-qabr, la ilaha illa ant (3x)",
                    "O Allah, grant me health in my body, hearing and sight; none has the right to be worshipped but You. I seek refuge in You from disbelief, poverty, and the punishment of the grave."},
            {"The master of seeking forgiveness",
                    "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
                    "Allahumma anta rabbi la ilaha illa ant, khalaqtani wa ana 'abduk, wa ana 'ala 'ahdika wa wa'dika mastata't, a'udhu bika min sharri ma sana't, abu'u laka bini'matika 'alayy, wa abu'u bidhanbi faghfir li, fa innahu la yaghfirudh-dhunuba illa ant",
                    "The master of seeking forgiveness — whoever says it in the evening with certainty and dies that night enters Paradise."},
            {"Pleased with Allah (3x)",
                    "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
                    "Raditu billahi rabban, wa bil-islami dinan, wa bi Muhammadin sallallahu 'alayhi wa sallam nabiyya (3x)",
                    "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad ﷺ as my Prophet."},
            {"Nothing can harm (3x)",
                    "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
                    "Bismillahilladhi la yadurru ma'a ismihi shay'un fil-ardi wa la fis-sama', wa huwas-sami'ul-'alim (3x)",
                    "In the name of Allah, with whose name nothing on earth or in heaven can cause harm; He is the All-Hearing, All-Knowing."},
            {"Refuge in His perfect words (3x)",
                    "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
                    "A'udhu bikalimatillah at-tammati min sharri ma khalaq (3x)",
                    "I seek refuge in the perfect words of Allah from the evil of what He created."},
            {"Creator of the heavens and earth",
                    "اللَّهُمَّ فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي، وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ",
                    "Allahumma fatiras-samawati wal-ard, 'alimal-ghaybi wash-shahadah, rabba kulli shay'in wa malikah, ash-hadu alla ilaha illa ant, a'udhu bika min sharri nafsi, wa min sharrish-shaytani wa shirkih",
                    "O Allah, Creator of the heavens and earth, Knower of the unseen and the seen, Lord and Sovereign of all things, I seek refuge in You from the evil of my soul and from the evil of Satan and his traps."},
            {"Pardon and well-being",
                    "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
                    "Allahumma inni as'alukal-'afwa wal-'afiyata fid-dunya wal-akhirah",
                    "O Allah, I ask You for pardon and well-being in this world and the Hereafter."},
        },
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 32);
        root.setPadding(pad, Ui.dp(this, 24), pad, pad);
        root.setBackgroundColor(Ui.BG);
        sv.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.label(this, "🤲 Duas & Adhkar", 30, Ui.TEXT);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        for (int c = 0; c < CATS.length; c++) {
            TextView sec = Ui.label(this, Lang.t(CATS[c]) + "  ("
                    + Lang.num(String.valueOf(DUAS[c].length)) + ")", 24, Ui.GOLD_BRIGHT);
            sec.setTypeface(sec.getTypeface(), android.graphics.Typeface.BOLD);
            Ui.margin(sec, 0, Ui.dp(this, c == 0 ? 16 : 32), 0, 0);
            root.addView(sec);
            for (String[] d : DUAS[c]) {
                root.addView(duaCard(d));
            }
        }
        // bottom spacer for D-pad scrolling
        TextView sp = Ui.label(this, "", 16, Ui.BG);
        Ui.margin(sp, 0, Ui.dp(this, 40), 0, 0);
        root.addView(sp);

        setContentView(sv);
    }

    private LinearLayout duaCard(String[] d) {
        LinearLayout card = Ui.card(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        Ui.margin(card, 0, Ui.dp(this, 16), 0, 0);
        TextView t = Ui.label(this, d[0], 20, Ui.GOLD);
        t.setGravity(Lang.ar() ? Gravity.RIGHT : Gravity.LEFT);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(t);
        TextView ar = Ui.label(this, d[1], 26, Ui.TEXT);
        ar.setGravity(Gravity.END);
        Ui.margin(ar, 0, Ui.dp(this, 12), 0, 0);
        card.addView(ar);
        TextView tr = Ui.label(this, d[2], 17, 0xFFFFE082);
        tr.setTypeface(tr.getTypeface(), android.graphics.Typeface.ITALIC);
        Ui.margin(tr, 0, Ui.dp(this, 10), 0, 0);
        card.addView(tr);
        TextView en = Ui.label(this, d[3], 17, Ui.DIM);
        Ui.margin(en, 0, Ui.dp(this, 6), 0, 0);
        card.addView(en);
        return card;
    }
}
