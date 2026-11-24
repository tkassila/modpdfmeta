Olet javafx arkkitehti tässä java-maven projektissa. Käytä hyväksi parhaat tavat ja design patternit. Tee koodista modulaarista ja helposti ylläpidettävää. Käytä selkeitä ja ytimekkäitä nimiä. Tee koodista selkeää ja helppoa lukea.

Käytä pdf käsittelyyn Apache PDFBox kirjastoa (=pom.xml tiedostossa jo) ja epub käsittelyyn Epublib kirjastoa (=pom.xml tiedostossa jo) sekä metatietojen käsittelyyn sekä väärien metatieto merkkijonojen käsittelyyn ja korjaukseen standardi merkkijonoksi com.github.albfernandez.juniversalchardet kirjastoa (=pom.xml tiedostossa jo) ja tiedostonimet muokataessa käytä java.nio.file.Path luokkaa ja sen metodeja tiedostonimien käsittelyyn. Älä käytä muita kirjastoja pdf tai epub käsittelyyn. Käytä kaikkia tarvittavia javafx kirjastoja ja luokkia.

1. Arkkitehtuuri ja periaatteet

Java-Maven, JavaFX, modulaarinen rakenne, tumma teema...

2. Päätoiminnallisuus

Matala ja korkea metatietojen haku ja mahdollinen metatietojen muutos,silloin kun on kyseessä pdf tiedosto, jota tarkastellaan jne. Tietojen siirto kenttäryhmien välillä ja tallennus pdf tiedostoon.

Kun tarkastellaan epub tiedostoa, niin kyseessä on sen muokkaus nmetatietoihin sekä mahdollinen tallennus. 

On olemassa kaksi eri tyyppisten metatietojen muokkaus näkymiä. Molempien näkymien perus rakenne on kuitenkin sama. Erot ovat lähinnä metatietojen kentissä ja niiden tyypeissä, joita on saatavilla. Muokattuja metatietoja voi tallentaa tiedostoon. Tallennetut metatiedot ovat tiedoston muokattuja metatietoja. Kummallakin tyypillä on oma tallenna tiedostoon painikkeen toiminnallisuutensa. 

Näytetään kaikki metatiedot dynaamisesti nimi ja arvo pareina, joita on mahdollista muokata. Kopioitaessa metatietoja pdf tiedoston yhteydessä ohjelma hakee vastaavan kenttä nimen, johon esim xml kentän arvo kopioidaan jne. Ja taas toisinpäin, kopioidaan esim pdf:n kenttä arvo xml kenttään. 

2.1 Käyttöliittymä: 1 gui näkymä

2.1.1 Ohjelman nimi (ikkunan otsikkorivillä, ei painonappipalkissa)
2.1.2 Painonappi bar, jossa avaa tiedosto ja valitse hakemisto painonapit sekä muut toimintopainikkeet (mm. 'Etsi metadataa tekstistä', 'Hae ai metadataa', kielivalitsin '🇫🇮 FI' ja '❓ Ohje'). Jokaisella painonapilla on sen tekstiä vastaava tooltip-ohjeteksti.
2.1.2.1 Otsikko: Avattu hakemisto tai avatun hakemisto polkuna.
2.1.3 Lista avatuista tiedostoista, jolla on tarvittaessa vierityspalkki ja josta voi valita avattavan tiedoston tai käyttää sen valitsemiseen edellinen ja seuraava painikkeita, jos käyttäjä on valinnut avattavan hakemiston.
2.1.3.1 Edellinen ja Seuraava painikkeet, jotka ovat aktiivisia vain, jos hakemisto on valittu ja listalla on enemmän kuin yksi tiedosto. 
2.1.3.2 Otsikko: avattu tiedosto.
2.1.3.3 Hiiren kaksoisklikkaus (2x) listan tiedostoon avaa kyseisen tiedoston käyttöjärjestelmän oletuskatseluohjelmalla (Linux: xdg-open / Desktop.open).
2.1.4 Listan alla on meta tietojen kentät, joissa voi tarkastella ja muokata metatietoja. Matalan tason paneelissa on merkistövalitsin (ComboBox: Automaattinen, UTF-8, Windows-1252, ISO-8859-1, UTF-16BE), jolla käyttäjä voi pakottaa toisen merkistön uudelleendekoodauksen, visuaalinen varoitusbanneri ei-standardista merkistöstä sekä erillinen 'Korjaa merkistö UTF:ksi' -painike, joka purkaa Mojibaken ja asettaa kentät tallennettavaksi standardina Unicode UTF-16BE -muodossa.
2.1.5 Kopioi xml metatiedoista matalan tason metakenttiin tiedot (koko ryhmä tai kenttäkohtaiset ◄ -nuolet).
2.1.6 Kopioi matalan tason metatiedoista xml tason metakenttiin tiedot (koko ryhmä tai kenttäkohtaiset ► -nuolet).
2.1.7 Meta tietojen kenttien alla on tallenna tiedostoon metatiedot.
2.1.7.1 Tallenna tiedostoon painikkeen aktivoituminen vaatii, että jotain on muokattu metatieto kenttään. Jos jotain on muokattu, niin painike on aktiivinen. Jos mitään ei ole muokattu, niin painike on passiivinen. Muutoksen kirjaus tapahtuu painike klikkauksen yhteydessä. 
2.1.8 Jos käyttäjä on siirtymässä toiseen tiedostoon, niin muuttuneista metatiedoista muistutetaan ja kysytään voidaanko tiedot tallentaa tiedostoon. Jos tiedot on tallennettu, niin siirrytään seuraavaan tiedostoon. Jos tiedot ei ole tallennettu, niin siirrytään seuraavaan tiedostoon ja tiedot hylätään.

3. Tiedostojen ja hakemistojen selaus

Yksittäisen tiedoston valinta
Kansion selaus (Seuraava / Edellinen, alihakemistot)

Käyttötapaus 1

Käytttäjä avaa 1 pdf tai epub tiedostoon ja tutkii sen metatietoja tai muokkaa niitä. Muokkauksen jälkeen tiedot tallennetaan pdf tiedostoon tai epub tiedostoon. Tiedostonimen muutos on myös mahdollista.

Käyttötapaus 2

Käytttäjä valitsee pdf tai epub hakemiston, jonka pdf tai epub tiedostoja tulee yksikerrallaan näytölle ja tutkii sen metatietoja tai muokkaa niitä. Muokkauksen jälkeen tiedot tallennetaan pdf tiedostoon tai epub tiedostoon. Tiedostonimen muutos on myös mahdollista. Tallenna aina ensisijaisesti tiedosto samaan kansioon. Seuraava painikkeen kautta siirrytään seuraavaan tiedostoon. Edellinen painikkeen kautta siirrytään edelliseen tiedostoon.

4. EPUB-vastaavuuden haku

Jos pdf tiedostosta ei löydy riittävän kattavia tai muulla tavoin soveltuvia metatietoja, niin ohjelma voi yrittää etsiä vastaavaa epub tiedostoa, ensin tiedoston base nimellä ja sitten muilla menetelmillä.Jos soveltuvaa epub tiedostoa löytyy, niin ohjelma näyttää epub tiedoston metatiedot yhtä aikaa näytöllä, jolloin käyttäjällä on mahdollisuus määrätä mitä pdf metatietoja - xml metatietoja tai raaka metatietoja kopioidaan epub metatietoihin. Tässä tilanteessa on aktiivisena tallenna epub tiedostoon.

5. Tehdyt muutokset ja korjauslinjaukset (Changelog / Decision log)

- Lisätty pom.xml:ään `com.positiondev.epublib:epublib-core:3.1` ja asetettu compiler target/source arvoon 21.
- Päivitetty pääluokat ja kontrollerit: `PdfEpubMetaWriterApplication` ja `PdfEpubMetaWriterController` (pakkauksessa `com.metait.modpdfmeta.controller`).
- Toteutettu modulaarinen palvelurakenne: `CharsetFixerService` (mojibake-purku, BOM-tarkistus, juniversalchardet), `PdfMetadataService` (PDFBox 3 COSDictionary ja XMP), `EpubMetadataService` (Epublib), `EpubMatcherService` (vastaavan EPUB:in haku).
- EPUB-vertailu ja haku: Ei automaattista kenttien kopiointia. Metatiedot tuodaan rinnakkaiselle paneelille vertailtavaksi, ja käyttäjä kopioi tiedot eksplisiittisesti painikkeilla (PDF <-> EPUB).
- Käyttöliittymä ja teema: `pdfepubmetawriter-view.fxml` ja moderni tumma tyyli `dark-theme.css`. Tallennuspainikkeet aktivoituvat vasta kun kenttiä muokataan, ja tiedostoa vaihdettaessa tallentamattomista muutoksista varoitetaan vahvistusdialogilla.
- Hakemistomuisti ja tiedostorajaukset: Avaa tiedosto ja Valitse hakemisto -dialogit avaavat oletuksena käyttäjän kotihakemiston ja muistavat viimeksi valitun kansion. Avaa tiedosto -dialogi listaa ainoastaan `.pdf`- ja `.epub`-tiedostot (ei muita tiedostoja / `*.*`). Valitse hakemisto tuo tiedostolistaan vain todelliset `.pdf`- ja `.epub`-tiedostot aakkosjärjestyksessä.
- Metadatan etsintä tekstistä: Lisätty oikeaan reunaan vaalean violetti painike `Etsi metadata tekstistä`. Palvelu `TextMetadataExtractorService` louhii tiedoston alun tekstistä puuttuvat tiedot (monirivinen nimike, pääotsikon alla olevat kirjoittajat muotoa Etunimi Sukunimi, kustantaja, painovuosi, ISBN/avainsanat, kieli). `MetadataExtractionDialog` kysyy kenttäkohtaisesti (Kyllä / Ei valintaruuduilla) mitkä arvot otetaan käyttöön. PDF-tiedostolle arvot viedään raaka data -kenttiin ja EPUB-tiedostolle sen omiin kenttiin.
- Avaaminen oletusohjelmalla: Tiedostolistan alkiota 2x hiirellä klikkaamalla tiedosto avataan suoraan käyttöjärjestelmän oletusohjelmalla (Linux: `xdg-open`, macOS: `open`, Windows: `start`).
- Kenttäkohtaiset siirtonuolet: Jokaisen metatietokentän (Title, Author, Publisher, Subject, Keywords, Date, Language) vieressä on nuolipainike yksittäisen arvon siirtoon toisentyyppiseen metatietokenttään (Matalalla tasolla `►` kopioi arvon XML-kenttään, ja XML-tasolla `◄` kopioi arvon matalan tason kenttään). Siirto päivittää muutostilan ja aktivoi tallennuspainikkeet.
- Manuaalinen merkistövalitsin ja varoitus: Matalan tason paneeliin lisätty pudotusvalikko (ComboBox: Automaattinen, UTF-8, Windows-1252, ISO-8859-1, UTF-16BE) raakatavujen uudelleendekoodaukseen. Jos alkuperäinen merkistö on ei-standardi (esim. Windows-1252, ISO-8859-1), käyttöliittymä näyttää selkeän varoituksen ja tarjoaa painikkeen 'Korjaa merkistö UTF:ksi'.
- Matalan tason Unicode-tallennus: `PdfMetadataService.tallennaMatalanTasonMetatiedot` tallentaa tiedot COSDictionaryyn eksplisiittisellä Unicode UTF-16BE -rakenteella (BOM `FE FF`), jolloin tallennettu tiedosto tunnistetaan jatkossa aina puhtaaksi Unicodeksi eikä varoitusta enää synny uudelleenavauksessa.
- Tekstin metadatan louhinta ja AI-haku: Painike 'Etsi metadata tekstistä' nimettiin muotoon 'Etsi metadataa tekstistä' ja siirrettiin työkalupalkissa vasemmalla olevan EPUB-hakupainikkeen viereen. Lisätty uusi painike 'Hae ai metadataa', joka hyödyntää `AiMetadataExtractorService`-palvelua (Gemini API / Ollama / semanttinen NLP) metatietojen erottelemiseen tekstistä. Molemmat avaavat saman 'Tekstistä löydetyt metatiedot' -dialogin, josta käyttäjä voi valita haluamansa kentät siirrettäväksi tiedoston metatietoihin.
- Virheilmoitusten leikepöytäkopiointi: `DialogHelper.naytaVirhe` sisältää 'Kopioi leikepöydälle' -painikkeen (sekä pitkille virheviesteille vieritettävän tekstialueen), jolla käyttäjä voi yhdellä klikkauksella kopioida esim. AI API -kutsun virheviestin tai HTTP-vastauksen leikepöydälle tarkastelua varten.
- Gemini AI -mallien päivitys ja vikasietoisuus: Korvattu vanhentunut `gemini-1.5-flash` dynaamisella mallijoukolla (`gemini-3.5-flash-lite`, `gemini-3.5-flash`, `gemini-3.8-flash`, `gemini-flash-latest`). Kutsu kokeilee malleja järjestyksessä automaattisella varamekanismilla (fallback), mikä takaa toimivuuden myös kysyntäpiikkien (503/429) tai mallimuutosten aikana ennen paikalliseen AI-louhintaan siirtymistä. Virheellinen API-avain nollataan asetuksista uuden syöttämistä varten.
- Tiedostolistan tyyppikorostus: Tiedostolistassa (`ListView<FileItem>`) jokaisen tiedoston tyyppitunniste `[PDF]` näytetään vaaleanpunaisella taustalla (`.badge-pdf`, `#ff8787`, tumma teksti) ja `[EPUB]` vihreällä taustalla (`.badge-epub`, `#2e7d32`, valkoinen teksti). Tiedoston muutosindikaattori `*` ja tiedostonimi pysyvät selkeinä ja luettavina sekä valitsemattomassa että valitussa tilassa. Double-click ja valintatoiminnot toimivat normaalisti.
- Monikielinen käyttöohje (Help) ja GitHub README.md: Luotu englanninkielinen kattava `README.md` GitHub-julkaisua varten. Työkalupalkkiin lisätty '❓ Ohje' -painike, joka avaa monikielisen käyttöohjedialogin (`HelpDialog`). Ohjeet saatavilla 6 tärkeimmällä eurooppalaisella kielellä (Suomi, English, Svenska, Deutsch, Français, Español) selkeinä välilehtinä. Teemoitettu sovelluksen tummalla tyylillä (`dark-theme.css`).
- Monikielinen käyttöliittymä ja dynaaminen GUI-kielen vaihto: Työkalupalkkiin lisätty kielivalitsin (ComboBox: `🇫🇮 FI`, `🇬🇧 EN`, `🇸🇪 SV`, `🇩🇪 DE`, `🇫🇷 FR`, `🇪🇸 ES`), josta valitsemalla koko käyttöliittymän kieli (painikkeet, kenttäotsikot, paneelien nimet, infotekstit ja tilapalkki) vaihtuu välittömästi kyseiselle kielelle. Valinta tallennetaan käyttäjän asetuksiin (`app_gui_language`), ja '❓ Ohje' -painike avaa suoraan aktiivista kieltä vastaavan ohjevälilehden. Toteutus luokassa `com.metait.modpdfmeta.util.I18n`.

