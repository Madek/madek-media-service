require "features/shared/authentication_error"

describe "Dashboard", type: :feature do
  let(:user) { create(:user, :with_system_admin_role) }
  let(:username) do
    person = user.person
    "#{person.first_name} #{person.last_name}"
  end
  let(:path) { "/media-service" }

  context "for public access" do
    before do
      visit path
    end

    it "doesn't display user's dropdown in navbar" do
      expect(page).not_to have_css(".navbar a.dropdown-toggle")
    end

    it_displays "authentication error"
  end

  context "for signed in user" do
    before do
      sign_in
      visit path
    end

    it "displays user's dropdown" do
      expect(page).to have_css(".navbar a.dropdown-toggle", text: username)
    end
  end
end

def expect_dashboard_data
  within ".top-resources" do
    expect(page).to have_link("Media-Stores", href: "/media-service/stores/")
    expect(page).to have_link("Settings", href: "/media-service/settings/")
    expect(page).to have_link("Uploads", href: "/media-service/uploads/")
  end

  within ".dev-and-build" do
    expect(page).to have_link("Dependency diagram", href: "/media-service/public/deps.svg")
    expect(page).to have_link(nil, href: "https://github.com/Madek/madek-media-service")
  end
end
