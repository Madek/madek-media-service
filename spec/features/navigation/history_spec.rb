feature "Navigation" do
  let!(:store) { create(:media_store, :database) }
  let(:user) { create(:user, :with_system_admin_role) }
  let(:person_2) { create(:person, first_name: "Liana", last_name: "Warner") }
  let(:person_3) { create(:person, first_name: "Clara", last_name: "Warner") }
  let!(:user_2) { create(:user, person: person_2, email: "liana@example.com") }
  let!(:user_3) { create(:user, person: person_3, email: "clara@example.com") }
  let(:group) { create(:group) }

  before { sign_in }
  
  before do
    group.users << [user_2, user_3]
    store.users << [user_2, user_3]
    store.groups << group
  end
  
  context "when query params' changes are triggered by UI" do
    scenario "URLs with query params are not persisted in the history as separate entries" do
      visit "/media-service/"

      within ".dropdown" do
        find(".dropdown-toggle").click

        within ".dropdown-menu" do
          click_link "Media-Stores"
        end
      end
      expect(page).to have_current_path("/media-service/stores/")

      click_link "2", href: "/media-service/stores/database/users/"
      expect(page).to have_current_path("/media-service/stores/database/users/")

      fill_in "term", with: "r"
      expect(page).to have_current_path("/media-service/stores/database/users/?term=r")

      fill_in "term", with: "ar"
      expect(page).to have_current_path("/media-service/stores/database/users/?term=ar")

      select "25", from: "per-page"
      expect(page)
        .to have_current_path("/media-service/stores/database/users/?term=ar&page=1&per-page=25")

      page.go_back
      expect(page).to have_current_path("/media-service/stores/")

      page.go_back
      expect(page).to have_current_path("/media-service/")
    end
  end

  context "when query params' changes are not triggered by UI" do
    scenario "every URL is persisted in the browser history" do
      visit "/media-service/"
      visit "/media-service/stores/"
      visit "/media-service/stores/database/users/"
      visit "/media-service/stores/database/groups/?including-user=#{user_3.id}"
      visit "/media-service/stores/database/groups/?including-user=#{user_2.id}"
      visit "/media-service/stores/database/groups/"

      page.go_back
      expect(page)
        .to have_current_path("/media-service/stores/database/groups/?including-user=#{user_2.id}")

      page.go_back
      expect(page)
        .to have_current_path("/media-service/stores/database/groups/?including-user=#{user_3.id}")

      page.go_back
      expect(page).to have_current_path("/media-service/stores/database/users/")

      page.go_back
      expect(page).to have_current_path("/media-service/stores/")

      page.go_back
      expect(page).to have_current_path("/media-service/")
    end
  end
end
